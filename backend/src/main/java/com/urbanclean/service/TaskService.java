package com.urbanclean.service;

import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.exception.custom.InvalidStateTransitionException;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .report(report)
                .location(report.getLocation())
                .category(report.getCategory())
                .state(TaskState.PENDIENTE)
                .priorityScore(priorityScore)
                .duplicateCount(0)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created: {} with priority score: {}", savedTask.getId(), priorityScore);

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
     * Update task state
     * Validates state transitions according to state machine rules
     * Returns the previous state for audit logging
     */
    @Transactional
    public Task updateState(UUID taskId, TaskState newState) {
        Task task = getTaskById(taskId);
        TaskState currentState = task.getState();

        // Validate state transition
        validateStateTransition(currentState, newState);

        log.info("Updating task {} state: {} -> {}", taskId, currentState, newState);
        
        // Update state
        task.setState(newState);

        return taskRepository.save(task);
    }
    
    /**
     * Get previous state before update (for audit logging)
     */
    public TaskState getPreviousState(Task task) {
        return task.getState();
    }

    /**
     * Validate state transition according to state machine rules
     * Valid transitions:
     * - PENDIENTE -> ASIGNADO
     * - ASIGNADO -> EN_PROGRESO
     * - EN_PROGRESO -> RESUELTO
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
            case RESUELTO:
                // No transitions allowed from RESUELTO
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
            BigDecimal newPriority = priorityCalculatorService.recalculatePriority(task.getReport());
            task.setPriorityScore(newPriority);
            taskRepository.save(task);
            log.debug("Task {} priority updated to {}", task.getId(), newPriority);
        });
        
        log.info("Priority recalculation completed");
    }
}
