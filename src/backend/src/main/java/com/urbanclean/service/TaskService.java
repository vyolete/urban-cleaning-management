package com.urbanclean.service;

import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.entity.User;
import com.urbanclean.event.TaskAssignedEvent;
import com.urbanclean.event.TaskResolvedEvent;
import com.urbanclean.exception.custom.InvalidStateTransitionException;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.repository.TaskRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for task management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final PriorityCalculatorService priorityCalculatorService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    /**
     * Create a task from a report
     * Calculates priority score and initializes state to PENDIENTE
     */
    @Transactional
    public Task createTask(Report report) {
        log.info("Creating task from report: {}", report.getId());

        // Calculate priority score
        BigDecimal priorityScore = priorityCalculatorService.calculatePriority(report);

        // Create task entity
        Task task = Task.builder()
                .primaryReport(report)
                .location(report.getLocation())
                .category(report.getCategory())
                .state(TaskState.PENDIENTE)
                .priorityScore(priorityScore)
                .duplicateCount(0)
                .country(report.getCountry())  // Copy country from report
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created: {} with priority score: {} for country: {}", 
                savedTask.getId(), priorityScore, 
                report.getCountry() != null ? report.getCountry().getName() : "N/A");

        return savedTask;
    }

    /**
     * Get task by ID
     */
    @Transactional(readOnly = true)
    public Task getTaskById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    /**
     * Assign task to an operator
     * Changes state to ASIGNADO and publishes TaskAssignedEvent
     */
    @Transactional
    public Task assignTask(UUID taskId, UUID operatorId) {
        log.info("Assigning task {} to operator {}", taskId, operatorId);
        
        Task task = getTaskById(taskId);
        
        // Validate current state
        if (task.getState() != TaskState.PENDIENTE) {
            throw new InvalidStateTransitionException(
                String.format("Task must be in PENDIENTE state to be assigned. Current state: %s", task.getState())
            );
        }
        
        // Get operator
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found: " + operatorId));
        
        // Assign task
        task.setAssignedOperator(operator);
        task.setState(TaskState.ASIGNADO);
        
        Task savedTask = taskRepository.save(task);
        
        // Publish event for notification
        String location = String.format("%.6f, %.6f", 
            task.getLocation().getY(), 
            task.getLocation().getX());
        
        eventPublisher.publishEvent(new TaskAssignedEvent(
            this,
            taskId,
            operatorId,
            task.getCategory().toString(),
            location,
            task.getPriorityScore().doubleValue()
        ));
        
        log.info("Task {} assigned to operator {} successfully", taskId, operatorId);
        return savedTask;
    }

    /**
     * Update task state
     * Validates state transitions according to state machine rules
     * Auto-assigns operator when transitioning to ASIGNADO
     * Returns the previous state for audit logging
     */
    @Transactional
    public Task updateState(UUID taskId, TaskState newState) {
        Task task = getTaskById(taskId);
        TaskState currentState = task.getState();

        // Validate state transition
        validateStateTransition(currentState, newState);

        log.info("Updating task {} state: {} -> {}", taskId, currentState, newState);
        
        // Auto-assign operator when transitioning to ASIGNADO
        if (newState == TaskState.ASIGNADO && task.getAssignedOperator() == null) {
            User currentUser = getCurrentUser();
            task.setAssignedOperator(currentUser);
            log.info("Auto-assigned task {} to operator {}", taskId, currentUser.getId());
        }
        
        // Set resolved timestamp when transitioning to RESUELTO
        if (newState == TaskState.RESUELTO) {
            task.setResolvedAt(java.time.LocalDateTime.now());
            log.info("Task {} marked as resolved at {}", taskId, task.getResolvedAt());
        }
        
        // Update state
        task.setState(newState);

        return taskRepository.save(task);
    }
    
    /**
     * Update task state with resolution evidence
     * Required when transitioning to RESUELTO state
     */
    @Transactional
    public Task updateStateWithEvidence(UUID taskId, TaskState newState, String evidence) {
        Task task = getTaskById(taskId);
        TaskState currentState = task.getState();

        // Validate state transition
        validateStateTransition(currentState, newState);
        
        // Validate evidence requirement for RESUELTO state
        if (newState == TaskState.RESUELTO) {
            if (evidence == null || evidence.trim().isEmpty()) {
                throw new IllegalArgumentException("Resolution evidence is required when marking task as resolved");
            }
            task.setResolutionEvidence(evidence);
            task.setResolvedAt(java.time.LocalDateTime.now()); // Set resolved timestamp for MTTR calculation
        }

        log.info("Updating task {} state: {} -> {} with evidence", taskId, currentState, newState);
        
        // Update state
        task.setState(newState);
        Task savedTask = taskRepository.save(task);
        
        // Publish TaskResolvedEvent if transitioning to RESUELTO
        if (newState == TaskState.RESUELTO && task.getPrimaryReport() != null 
            && task.getPrimaryReport().getSubmitter() != null) {
            
            String citizenEmail = task.getPrimaryReport().getSubmitter().getEmail();
            String taskCategory = task.getCategory() != null ? task.getCategory().toString() : "Unknown";
            String taskDescription = task.getPrimaryReport().getDescription();
            
            log.info("Publishing TaskResolvedEvent for task {}", taskId);
            eventPublisher.publishEvent(new TaskResolvedEvent(
                this,
                taskId,
                citizenEmail,
                taskCategory,
                taskDescription
            ));
        }

        return savedTask;
    }
    
    /**
     * Get previous state before update (for audit logging)
     */
    public TaskState getPreviousState(Task task) {
        return task.getState();
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }

    /**
     * Validate state transition according to state machine rules
     * Valid transitions:
     * - PENDIENTE -> ASIGNADO
     * - ASIGNADO -> EN_PROGRESO
     * - EN_PROGRESO -> RESUELTO
     * - REABIERTO -> EN_PROGRESO
     */
    private void validateStateTransition(TaskState currentState, TaskState newState) {
        if (currentState == newState) {
            throw new InvalidStateTransitionException(
                String.format("Task is already in state: %s", currentState)
            );
        }

        boolean isValidTransition = false;

        switch (currentState) {
            case PENDIENTE:
                isValidTransition = (newState == TaskState.ASIGNADO);
                break;
            case ASIGNADO:
                isValidTransition = (newState == TaskState.EN_PROGRESO);
                break;
            case EN_PROGRESO:
                isValidTransition = (newState == TaskState.RESUELTO);
                break;
            case REABIERTO:
                // Reopened tasks go back to EN_PROGRESO
                isValidTransition = (newState == TaskState.EN_PROGRESO);
                break;
            case RESUELTO:
                // No manual transitions allowed from RESUELTO
                // State changes to REABIERTO are handled by FeedbackService
                isValidTransition = false;
                break;
        }

        if (!isValidTransition) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition: %s -> %s", currentState, newState)
            );
        }
    }

    /**
     * Recalculate priority for all pending tasks
     * Used when algorithm weights are updated
     */
    @Transactional
    public void recalculatePendingTasksPriority() {
        log.info("Recalculating priority for all pending tasks");
        
        taskRepository.findByState(TaskState.PENDIENTE).forEach(task -> {
            BigDecimal newPriority = priorityCalculatorService.recalculatePriority(task.getPrimaryReport());
            task.setPriorityScore(newPriority);
            taskRepository.save(task);
            log.debug("Task {} priority updated to {}", task.getId(), newPriority);
        });
        
        log.info("Priority recalculation completed");
    }
}
