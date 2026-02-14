package com.urbanclean.repository;

import com.urbanclean.entity.NotificationFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationFailureRepository extends JpaRepository<NotificationFailure, UUID> {
    
    /**
     * Find all notification failures for a user, ordered by most recent first
     * @param userId the user ID
     * @return list of notification failures
     */
    List<NotificationFailure> findByUserIdOrderByAttemptedAtDesc(UUID userId);
    
    /**
     * Find all notification failures before a specific date
     * @param date the cutoff date
     * @return list of notification failures
     */
    List<NotificationFailure> findByAttemptedAtBefore(LocalDateTime date);
    
    /**
     * Delete all notification failures before a specific date
     * @param date the cutoff date
     */
    void deleteByAttemptedAtBefore(LocalDateTime date);
}
