package com.urbanclean.repository;

import com.urbanclean.entity.Task;
import com.urbanclean.entity.TaskState;
import com.urbanclean.entity.User;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Task entity operations
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Find tasks by state ordered by priority score descending
     * @param state the task state to filter by
     * @return list of tasks ordered by priority
     */
    List<Task> findByStateOrderByPriorityScoreDesc(TaskState state);

    /**
     * Find tasks by state
     * @param state the task state to filter by
     * @return list of tasks
     */
    List<Task> findByState(TaskState state);

    /**
     * Find tasks by primary report ID
     * Returns list to avoid NonUniqueResultException from Hibernate JOINs
     * @param reportId the ID of the report associated with the task
     * @return list of tasks (should contain 0 or 1 element)
     */
    @Query("SELECT t FROM Task t WHERE t.primaryReport.id = :reportId")
    List<Task> findByReportId(@Param("reportId") UUID reportId);
    
    /**
     * Find task by report (deprecated - use findByReportId instead)
     * @param report the report associated with the task
     * @return optional containing the task
     */
    @Deprecated
    @Query("SELECT t FROM Task t WHERE t.primaryReport = :report")
    Optional<Task> findByReport(@Param("report") com.urbanclean.entity.Report report);

    /**
     * Find all tasks ordered by priority score descending
     * @return list of all tasks ordered by priority
     */
    List<Task> findAllByOrderByPriorityScoreDesc();

    /**
     * Find tasks assigned to an operator
     * @param operator the assigned operator
     * @return list of tasks assigned to the operator
     */
    List<Task> findByAssignedOperator(User operator);

    /**
     * Find tasks by state and assigned operator
     * @param state the task state
     * @param operator the assigned operator
     * @return list of matching tasks
     */
    List<Task> findByStateAndAssignedOperator(TaskState state, User operator);

    /**
     * Find tasks within a geographic zone ordered by priority
     * @param zone the geographic boundary
     * @return list of tasks within the zone
     */
    @Query(value = "SELECT t FROM Task t WHERE " +
           "ST_Within(t.location, :zone) = true " +
           "ORDER BY t.priorityScore DESC")
    List<Task> findTasksInZone(@Param("zone") Geometry zone);

    /**
     * Find tasks by state within a geographic zone
     * @param state the task state
     * @param zone the geographic boundary
     * @return list of matching tasks
     */
    @Query(value = "SELECT t FROM Task t WHERE " +
           "t.state = :state AND " +
           "ST_Within(t.location, :zone) = true " +
           "ORDER BY t.priorityScore DESC")
    List<Task> findByStateInZone(
        @Param("state") TaskState state,
        @Param("zone") Geometry zone
    );

    /**
     * Find pending tasks (for recalculation)
     * @return list of tasks not yet resolved
     */
    @Query("SELECT t FROM Task t WHERE t.state != 'RESUELTO'")
    List<Task> findPendingTasks();

    /**
     * Count tasks by state
     * @param state the task state
     * @return number of tasks in the state
     */
    long countByState(TaskState state);
    
    /**
     * Find tasks by country
     * @param countryId the country ID
     * @return list of tasks from the country
     */
    @Query("SELECT t FROM Task t WHERE t.country.id = :countryId ORDER BY t.priorityScore DESC")
    List<Task> findByCountryId(@Param("countryId") UUID countryId);
    
    /**
     * Find tasks by country and state
     * @param countryId the country ID
     * @param state the task state
     * @return list of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.country.id = :countryId AND t.state = :state ORDER BY t.priorityScore DESC")
    List<Task> findByCountryIdAndState(@Param("countryId") UUID countryId, @Param("state") TaskState state);
    
    /**
     * Find tasks by country and category
     * @param countryId the country ID
     * @param category the category
     * @return list of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.country.id = :countryId AND t.category = :category ORDER BY t.priorityScore DESC")
    List<Task> findByCountryIdAndCategory(@Param("countryId") UUID countryId, @Param("category") String category);
    
    // ========== ANALYTICS METHODS ==========
    
    /**
     * Count tasks by category within date range
     * Returns array of [category, count]
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of category counts
     */
    @Query("SELECT t.category, COUNT(t) FROM Task t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category " +
           "ORDER BY COUNT(t) DESC")
    List<Object[]> countByCategory(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
    
    /**
     * Count tasks by state within date range
     * Returns array of [state, count]
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of state counts
     */
    @Query("SELECT t.state, COUNT(t) FROM Task t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY t.state " +
           "ORDER BY COUNT(t) DESC")
    List<Object[]> countByState(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
    
    /**
     * Find resolved tasks within date range for MTTR calculation
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of resolved tasks
     */
    @Query("SELECT t FROM Task t " +
           "WHERE t.state = 'RESUELTO' " +
           "AND t.resolvedAt BETWEEN :startDate AND :endDate")
    List<Task> findResolvedTasks(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
    
    /**
     * Get operator statistics within date range
     * Returns array of [operatorId, username, tasksResolved, avgResolutionTime, tasksInProgress, tasksReopened, activeSince]
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of operator statistics
     */
    @Query(value = "SELECT " +
           "u.id, " +
           "u.username, " +
           "SUM(CASE WHEN t.state = 'RESUELTO' THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN t.state = 'RESUELTO' AND t.resolved_at IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (t.resolved_at - t.created_at)) / 3600.0 ELSE NULL END), " +
           "SUM(CASE WHEN t.state IN ('ASIGNADO', 'EN_PROGRESO') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.state = 'REABIERTO' THEN 1 ELSE 0 END), " +
           "u.created_at " +
           "FROM tareas t " +
           "JOIN usuarios u ON t.assigned_operator_id = u.id " +
           "WHERE t.assigned_operator_id IS NOT NULL " +
           "AND t.created_at BETWEEN :startDate AND :endDate " +
           "GROUP BY u.id, u.username, u.created_at " +
           "ORDER BY SUM(CASE WHEN t.state = 'RESUELTO' THEN 1 ELSE 0 END) DESC",
           nativeQuery = true)
    List<Object[]> getOperatorStatistics(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
}
