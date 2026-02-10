package com.urbanclean.event;

import com.urbanclean.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for task-related events
 * Handles email notifications asynchronously
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventListener {

    private final EmailService emailService;

    /**
     * Listen for TaskResolvedEvent and send email to citizen
     * Executed asynchronously to avoid blocking the main thread
     */
    @Async
    @EventListener
    public void handleTaskResolved(TaskResolvedEvent event) {
        log.info("Handling TaskResolvedEvent for task: {}", event.getTaskId());
        
        try {
            emailService.sendTaskResolvedEmail(
                event.getCitizenEmail(),
                event.getTaskId().toString(),
                event.getTaskCategory()
            );
            
            log.info("Task resolved email sent to: {}", event.getCitizenEmail());
        } catch (Exception e) {
            log.error("Failed to send task resolved email to {}: {}", 
                event.getCitizenEmail(), e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }

    /**
     * Listen for TaskReopenedEvent and send email to operator
     * Executed asynchronously to avoid blocking the main thread
     */
    @Async
    @EventListener
    public void handleTaskReopened(TaskReopenedEvent event) {
        log.info("Handling TaskReopenedEvent for task: {}", event.getTaskId());
        
        try {
            emailService.sendTaskReopenedEmail(
                event.getOperatorEmail(),
                event.getTaskId().toString(),
                event.getTaskCategory(),
                event.getRejectionJustification()
            );
            
            log.info("Task reopened email sent to: {}", event.getOperatorEmail());
        } catch (Exception e) {
            log.error("Failed to send task reopened email to {}: {}", 
                event.getOperatorEmail(), e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }
}
