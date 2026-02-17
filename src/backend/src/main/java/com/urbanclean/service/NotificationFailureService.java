package com.urbanclean.service;

import com.urbanclean.entity.NotificationFailure;
import com.urbanclean.repository.NotificationFailureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationFailureService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationFailureService.class);

    @Autowired
    private NotificationFailureRepository notificationFailureRepository;

    /**
     * Record a notification failure
     */
    @Transactional
    public void recordFailure(UUID userId, String notificationType, String emailAddress, 
                             String failureReason, Integer retryCount) {
        NotificationFailure failure = new NotificationFailure(
            userId, notificationType, emailAddress, failureReason, retryCount
        );
        
        notificationFailureRepository.save(failure);
        logger.info("Recorded notification failure for user: {}, type: {}", userId, notificationType);
    }

    /**
     * Get all notification failures with optional filtering
     */
    @Transactional(readOnly = true)
    public List<NotificationFailure> getFailures(LocalDateTime startDate, LocalDateTime endDate, 
                                                 String notificationType, UUID userId) {
        // For now, return all failures
        // TODO: Implement filtering in repository
        return notificationFailureRepository.findAll();
    }

    /**
     * Get failures for a specific user
     */
    @Transactional(readOnly = true)
    public List<NotificationFailure> getFailuresByUser(UUID userId) {
        return notificationFailureRepository.findByUserIdOrderByAttemptedAtDesc(userId);
    }

    /**
     * Retry a failed notification
     */
    @Transactional
    public void retryFailedNotification(UUID failureId) {
        // TODO: Implement retry logic
        logger.info("Retry requested for notification failure: {}", failureId);
    }

    /**
     * Clean up old notification failures (older than 30 days)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldFailures() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        logger.info("Cleaning up notification failures older than {}", cutoffDate);
        
        List<NotificationFailure> oldFailures = notificationFailureRepository.findByAttemptedAtBefore(cutoffDate);
        int count = oldFailures.size();
        
        if (count > 0) {
            notificationFailureRepository.deleteByAttemptedAtBefore(cutoffDate);
            logger.info("Deleted {} old notification failures", count);
        } else {
            logger.info("No old notification failures to clean up");
        }
    }
}
