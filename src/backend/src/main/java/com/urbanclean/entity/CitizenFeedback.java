package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing citizen feedback on task resolution
 * Citizens have 72 hours to provide feedback after task is marked as resolved
 */
@Entity
@Table(name = "citizen_feedback", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feedback_task", columnNames = "task_id")
    },
    indexes = {
        @Index(name = "idx_feedback_deadline", columnList = "feedback_deadline")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType type;

    @Column(length = 500)
    private String justification;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(nullable = false, name = "feedback_deadline")
    private LocalDateTime feedbackDeadline;

    /**
     * Check if feedback deadline has passed
     */
    public boolean isDeadlinePassed() {
        return LocalDateTime.now().isAfter(feedbackDeadline);
    }
}
