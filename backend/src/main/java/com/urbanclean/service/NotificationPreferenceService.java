package com.urbanclean.service;

import com.urbanclean.entity.NotificationPreference;
import com.urbanclean.enums.NotificationType;
import com.urbanclean.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    /**
     * Get notification preferences for a user
     * If preferences don't exist, create default preferences
     * @param userId the user ID
     * @return the notification preferences
     */
    @Transactional
    public NotificationPreference getPreferences(UUID userId) {
        return notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Update notification preferences for a user
     * @param userId the user ID
     * @param taskAssigned enable/disable task assigned notifications
     * @param taskResolved enable/disable task resolved notifications
     * @param taskReopened enable/disable task reopened notifications
     * @param reportCreated enable/disable report created notifications
     * @return the updated preferences
     */
    @Transactional
    public NotificationPreference updatePreferences(UUID userId, Boolean taskAssigned, 
                                                   Boolean taskResolved, Boolean taskReopened, 
                                                   Boolean reportCreated) {
        NotificationPreference preferences = getPreferences(userId);
        
        if (taskAssigned != null) {
            preferences.setTaskAssigned(taskAssigned);
        }
        if (taskResolved != null) {
            preferences.setTaskResolved(taskResolved);
        }
        if (taskReopened != null) {
            preferences.setTaskReopened(taskReopened);
        }
        if (reportCreated != null) {
            preferences.setReportCreated(reportCreated);
        }
        
        NotificationPreference saved = notificationPreferenceRepository.save(preferences);
        logger.info("Updated notification preferences for user: {}", userId);
        return saved;
    }

    /**
     * Check if a specific notification type is enabled for a user
     * @param userId the user ID
     * @param type the notification type
     * @return true if enabled, false otherwise
     */
    public boolean isNotificationEnabled(UUID userId, NotificationType type) {
        NotificationPreference preferences = getPreferences(userId);
        
        return switch (type) {
            case TASK_ASSIGNED -> preferences.getTaskAssigned();
            case TASK_RESOLVED -> preferences.getTaskResolved();
            case TASK_REOPENED -> preferences.getTaskReopened();
            case REPORT_CREATED -> preferences.getReportCreated();
        };
    }

    /**
     * Create default notification preferences for a user
     * All notification types are enabled by default
     * @param userId the user ID
     * @return the created preferences
     */
    @Transactional
    public NotificationPreference createDefaultPreferences(UUID userId) {
        NotificationPreference preferences = new NotificationPreference(userId);
        NotificationPreference saved = notificationPreferenceRepository.save(preferences);
        logger.info("Created default notification preferences for user: {}", userId);
        return saved;
    }
}
