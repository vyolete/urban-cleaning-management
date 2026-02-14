package com.urbanclean.listener;

import com.urbanclean.entity.User;
import com.urbanclean.enums.NotificationType;
import com.urbanclean.event.TaskAssignedEvent;
import com.urbanclean.repository.UserRepository;
import com.urbanclean.service.EmailService;
import com.urbanclean.service.NotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listener for task assignment events
 * Sends email notifications to operators when tasks are assigned
 */
@Component
public class TaskAssignmentListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentListener.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Handle task assigned event
     * Sends email notification if user has notifications enabled
     */
    @EventListener
    @Async
    public void handleTaskAssigned(TaskAssignedEvent event) {
        try {
            logger.info("Processing task assignment event for task: {} and operator: {}", 
                       event.getTaskId(), event.getOperatorId());

            // Check if operator has notifications enabled
            boolean notificationsEnabled = notificationPreferenceService.isNotificationEnabled(
                event.getOperatorId(), 
                NotificationType.TASK_ASSIGNED
            );

            if (!notificationsEnabled) {
                logger.info("Notifications disabled for operator: {}, skipping email", 
                           event.getOperatorId());
                return;
            }

            // Get operator details
            Optional<User> operatorOpt = userRepository.findById(event.getOperatorId());
            if (operatorOpt.isEmpty()) {
                logger.warn("Operator not found: {}", event.getOperatorId());
                return;
            }

            User operator = operatorOpt.get();

            // Send email notification
            emailService.sendTaskAssignmentEmail(
                operator.getEmail(),
                event.getTaskId().toString(),
                event.getCategory(),
                event.getLocation(),
                event.getPriorityScore(),
                operator.getUsername()
            );

            logger.info("Task assignment notification sent to operator: {}", operator.getEmail());

        } catch (Exception e) {
            logger.error("Error processing task assignment event: {}", e.getMessage(), e);
            // Don't rethrow - we don't want to fail the task assignment if email fails
        }
    }
}
