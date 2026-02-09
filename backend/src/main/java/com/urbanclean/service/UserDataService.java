package com.urbanclean.service;

import com.urbanclean.entity.User;
import com.urbanclean.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for GDPR-compliant user data management
 * Handles account deletion, anonymization, and data export
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDataService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    private static final int DELETION_GRACE_PERIOD_DAYS = 7;

    /**
     * Request account deletion
     * Starts a 7-day grace period before anonymization
     * 
     * @param userId ID of the user requesting deletion
     * @param password User's password for confirmation
     * @return true if deletion was requested successfully
     */
    @Transactional
    public boolean requestAccountDeletion(UUID userId, String password) {
        log.info("Account deletion requested for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SecurityException("Invalid password");
        }
        
        // Check if already deleted
        if (user.getDeletedAt() != null) {
            throw new IllegalStateException("Account deletion already requested");
        }
        
        // Check if already anonymized
        if (user.getAnonymized()) {
            throw new IllegalStateException("Account is already anonymized");
        }
        
        // Set deletion timestamp (starts grace period)
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Send confirmation email
        emailService.sendAccountDeletionConfirmationEmail(user.getEmail(), user.getUsername());
        
        log.info("Account deletion requested for user: {}. Grace period ends: {}", 
            userId, user.getDeletedAt().plusDays(DELETION_GRACE_PERIOD_DAYS));
        
        return true;
    }

    /**
     * Cancel account deletion request
     * Can only be done during the 7-day grace period
     * 
     * @param userId ID of the user
     * @return true if cancellation was successful
     */
    @Transactional
    public boolean cancelAccountDeletion(UUID userId) {
        log.info("Canceling account deletion for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if deletion was requested
        if (user.getDeletedAt() == null) {
            throw new IllegalStateException("No deletion request found");
        }
        
        // Check if already anonymized
        if (user.getAnonymized()) {
            throw new IllegalStateException("Account is already anonymized and cannot be recovered");
        }
        
        // Check if grace period has passed
        LocalDateTime gracePeriodEnd = user.getDeletedAt().plusDays(DELETION_GRACE_PERIOD_DAYS);
        if (LocalDateTime.now().isAfter(gracePeriodEnd)) {
            throw new IllegalStateException("Grace period has expired");
        }
        
        // Cancel deletion
        user.setDeletedAt(null);
        userRepository.save(user);
        
        log.info("Account deletion canceled for user: {}", userId);
        
        return true;
    }

    /**
     * Anonymize user data after grace period
     * Scheduled to run daily at 3 AM
     * Preserves historical data while removing PII
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void anonymizeUserData() {
        log.info("Starting scheduled anonymization job");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DELETION_GRACE_PERIOD_DAYS);
        
        // Find users past grace period
        List<User> usersToAnonymize = userRepository.findAll().stream()
            .filter(user -> user.getDeletedAt() != null)
            .filter(user -> !user.getAnonymized())
            .filter(user -> user.getDeletedAt().isBefore(cutoffDate))
            .toList();
        
        int anonymizedCount = 0;
        for (User user : usersToAnonymize) {
            try {
                anonymizeUser(user);
                anonymizedCount++;
            } catch (Exception e) {
                log.error("Failed to anonymize user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Anonymization job completed. Anonymized {} users", anonymizedCount);
    }

    /**
     * Anonymize a single user's data
     * Replaces PII with anonymized values while preserving historical records
     * 
     * @param user User to anonymize
     */
    @Transactional
    public void anonymizeUser(User user) {
        log.info("Anonymizing user: {}", user.getId());
        
        // Hash original email for audit trail
        String emailHash = hashEmail(user.getEmail());
        user.setOriginalEmailHash(emailHash);
        
        // Replace username with anonymized version
        String anonymizedUsername = "usuario_anonimo_" + emailHash.substring(0, 8);
        user.setUsername(anonymizedUsername);
        
        // Replace email with hashed identifier
        String anonymizedEmail = "deleted_" + emailHash + "@anonymized.local";
        user.setEmail(anonymizedEmail);
        
        // Clear password hash
        user.setPasswordHash("");
        
        // Mark as anonymized
        user.setAnonymized(true);
        
        userRepository.save(user);
        
        log.info("User {} anonymized successfully. New username: {}", 
            user.getId(), anonymizedUsername);
        
        // Note: Historical reports remain linked to the user ID
        // but the user's PII is now removed
    }

    /**
     * Hash email using SHA-256
     * Used for creating anonymized identifiers
     */
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if user is in deletion grace period
     */
    public boolean isInDeletionGracePeriod(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (user.getDeletedAt() == null) {
            return false;
        }
        
        LocalDateTime gracePeriodEnd = user.getDeletedAt().plusDays(DELETION_GRACE_PERIOD_DAYS);
        return LocalDateTime.now().isBefore(gracePeriodEnd);
    }

    /**
     * Get remaining days in grace period
     */
    public long getRemainingGracePeriodDays(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (user.getDeletedAt() == null) {
            return 0;
        }
        
        LocalDateTime gracePeriodEnd = user.getDeletedAt().plusDays(DELETION_GRACE_PERIOD_DAYS);
        long daysRemaining = java.time.Duration.between(LocalDateTime.now(), gracePeriodEnd).toDays();
        
        return Math.max(0, daysRemaining);
    }
}
