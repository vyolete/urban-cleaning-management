package com.urbanclean.repository;

import com.urbanclean.entity.CitizenFeedback;
import com.urbanclean.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for citizen feedback operations
 */
@Repository
public interface CitizenFeedbackRepository extends JpaRepository<CitizenFeedback, UUID> {

    /**
     * Find feedback by task ID
     */
    Optional<CitizenFeedback> findByTaskId(UUID taskId);

    /**
     * Find all tasks with pending feedback past deadline
     * Used for auto-closing tasks after 72 hours
     */
    @Query("SELECT cf FROM CitizenFeedback cf " +
           "WHERE cf.feedbackDeadline < :now " +
           "AND cf.task.state = 'RESUELTO'")
    List<CitizenFeedback> findPendingFeedbackPastDeadline(LocalDateTime now);

    /**
     * Check if feedback exists for a task
     */
    boolean existsByTask(Task task);
}
