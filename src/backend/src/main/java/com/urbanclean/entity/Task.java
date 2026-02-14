package com.urbanclean.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Task entity representing work items created from reports
 */
@Entity
@Table(name = "tareas", indexes = {
    @Index(name = "idx_task_location", columnList = "location"),
    @Index(name = "idx_task_state", columnList = "state"),
    @Index(name = "idx_task_priority", columnList = "priority_score")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_report_id", nullable = false)
    private Report primaryReport;

    @OneToMany(mappedBy = "parentTask", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Report> duplicateReports = new ArrayList<>();

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskState state;

    @Column(nullable = false, precision = 10, scale = 2, name = "priority_score")
    private BigDecimal priorityScore;

    @Column(nullable = false, name = "duplicate_count")
    @Builder.Default
    private Integer duplicateCount = 0;

    @Column(length = 1000, name = "resolution_evidence")
    private String resolutionEvidence;

    @Column(name = "reopen_count")
    @Builder.Default
    private Integer reopenCount = 0;

    @Column(name = "citizen_approved")
    @Builder.Default
    private Boolean citizenApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedOperator;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
