package com.urbanclean.service;

import com.urbanclean.entity.*;
import com.urbanclean.event.TaskReopenedEvent;
import com.urbanclean.repository.CitizenFeedbackRepository;
import com.urbanclean.repository.TaskRepository;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing citizen feedback on task resolutions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final CitizenFeedbackRepository feedbackRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    
    private static final int FEEDBACK_DEADLINE_HOURS = 72;
    private static final int MAX_REOPEN_COUNT = 3;

    /**
     * Confirm task resolution
     * Citizen approves that the task has been resolved satisfactorily
     * 
     * @param taskId ID of the task
     * @param citizenId ID of the citizen providing feedback
     * @return true if feedback was recorded successfully
     */
    @Transactional
    public boolean confirmResolution(UUID taskId, UUID citizenId) {
        log.info("Citizen {} confirming resolution of task {}", citizenId, taskId);
        
        // Validate task and citizen
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        User citizen = userRepository.findById(citizenId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate authorization (only original reporter can provide feedback)
        if (!task.getPrimaryReport().getSubmitter().getId().equals(citizenId)) {
            log.warn("Unauthorized feedback attempt by user {} for task {}", citizenId, taskId);
            throw new SecurityException("Only the original reporter can provide feedback");
        }
        
        // Validate task state
        if (task.getState() != TaskState.RESUELTO) {
            throw new IllegalStateException("Task must be in RESUELTO state to receive feedback");
        }
        
        // Check if feedback already exists
        if (feedbackRepository.existsByTask(task)) {
            throw new IllegalStateException("Feedback already provided for this task");
        }
        
        // Create feedback
        CitizenFeedback feedback = CitizenFeedback.builder()
            .task(task)
            .citizen(citizen)
            .type(FeedbackType.CONFIRMED)
            .feedbackDeadline(LocalDateTime.now().plusHours(FEEDBACK_DEADLINE_HOURS))
            .build();
        
        feedbackRepository.save(feedback);
        
        // Update task
        task.setCitizenApproved(true);
        taskRepository.save(task);
        
        log.info("Task {} resolution confirmed by citizen {}", taskId, citizenId);
        return true;
    }

    /**
     * Reject task resolution and reopen the task
     * Citizen indicates the task is not actually resolved
     * 
     * @param taskId ID of the task
     * @param citizenId ID of the citizen providing feedback
     * @param justification Reason for rejection (required)
     * @return true if feedback was recorded and task reopened
     */
    @Transactional
    public boolean rejectResolution(UUID taskId, UUID citizenId, String justification) {
        log.info("Citizen {} rejecting resolution of task {}", citizenId, taskId);
        
        // Validate justification
        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justification is required when rejecting resolution");
        }
        
        // Validate task and citizen
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        User citizen = userRepository.findById(citizenId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate authorization (only original reporter can provide feedback)
        if (!task.getPrimaryReport().getSubmitter().getId().equals(citizenId)) {
            log.warn("Unauthorized feedback attempt by user {} for task {}", citizenId, taskId);
            throw new SecurityException("Only the original reporter can provide feedback");
        }
        
        // Validate task state
        if (task.getState() != TaskState.RESUELTO) {
            throw new IllegalStateException("Task must be in RESUELTO state to receive feedback");
        }
        
        // Check if feedback already exists
        if (feedbackRepository.existsByTask(task)) {
            throw new IllegalStateException("Feedback already provided for this task");
        }
        
        // Check reopen limit
        if (task.getReopenCount() >= MAX_REOPEN_COUNT) {
            log.warn("Task {} has been reopened {} times, flagging for review", 
                taskId, task.getReopenCount());
            // Task will be flagged but still reopened
        }
        
        // Create feedback
        CitizenFeedback feedback = CitizenFeedback.builder()
            .task(task)
            .citizen(citizen)
            .type(FeedbackType.REJECTED)
            .justification(justification)
            .feedbackDeadline(LocalDateTime.now().plusHours(FEEDBACK_DEADLINE_HOURS))
            .build();
        
        feedbackRepository.save(feedback);
        
        // Reopen task
        task.setState(TaskState.REABIERTO);
        task.setReopenCount(task.getReopenCount() + 1);
        task.setCitizenApproved(false);
        taskRepository.save(task);
        
        // Publish TaskReopenedEvent to notify assigned operator
        if (task.getAssignedOperator() != null) {
            String operatorEmail = task.getAssignedOperator().getEmail();
            String taskCategory = task.getCategory() != null ? task.getCategory().toString() : "Unknown";
            String taskDescription = task.getPrimaryReport().getDescription();
            
            log.info("Publishing TaskReopenedEvent for task {}", taskId);
            eventPublisher.publishEvent(new TaskReopenedEvent(
                this,
                taskId,
                operatorEmail,
                taskCategory,
                taskDescription,
                justification
            ));
        }
        
        log.info("Task {} reopened by citizen {}, reopen count: {}", 
            taskId, citizenId, task.getReopenCount());
        
        return true;
    }

    /**
     * Auto-close tasks that have passed the 72-hour feedback deadline
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoCloseTasks() {
        log.info("Starting auto-close job for tasks past feedback deadline");
        
        List<CitizenFeedback> expiredFeedbacks = 
            feedbackRepository.findPendingFeedbackPastDeadline(LocalDateTime.now());
        
        int closedCount = 0;
        for (CitizenFeedback feedback : expiredFeedbacks) {
            Task task = feedback.getTask();
            
            // Only auto-close if still in RESUELTO state and not approved
            if (task.getState() == TaskState.RESUELTO && !task.getCitizenApproved()) {
                task.setCitizenApproved(true); // Auto-approve after deadline
                taskRepository.save(task);
                closedCount++;
                
                log.debug("Auto-closed task {} after feedback deadline", task.getId());
            }
        }
        
        log.info("Auto-closed {} tasks past feedback deadline", closedCount);
    }
}
