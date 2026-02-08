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
     * Find task by report
     * @param report the report associated with the task
     * @return optional containing the task
     */
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
}
