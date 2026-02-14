package com.urbanclean.repository;

import com.urbanclean.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    
    /**
     * Find notification preferences by user ID
     * @param userId the user ID
     * @return Optional containing the notification preferences if found
     */
    Optional<NotificationPreference> findByUserId(UUID userId);
    
    /**
     * Check if notification preferences exist for a user
     * @param userId the user ID
     * @return true if preferences exist, false otherwise
     */
    boolean existsByUserId(UUID userId);
}
