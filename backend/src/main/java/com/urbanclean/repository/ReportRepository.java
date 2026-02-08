package com.urbanclean.repository;

import com.urbanclean.entity.Report;
import com.urbanclean.entity.Task;
import com.urbanclean.entity.User;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Report entity operations with PostGIS spatial queries
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find reports by submitter
     * @param submitter the user who submitted the reports
     * @return list of reports
     */
    List<Report> findBySubmitter(User submitter);

    /**
     * Find reports by parent task
     * @param parentTask the parent task
     * @return list of reports grouped under the task
     */
    List<Report> findByParentTask(Task parentTask);

    /**
     * Find reports within distance threshold using PostGIS ST_DWithin
     * @param location the reference point
     * @param distanceMeters the distance threshold in meters
     * @param since the time threshold
     * @return list of spatially and temporally proximate reports
     */
    @Query(value = "SELECT r.* FROM reportes r WHERE " +
           "ST_DWithin(r.location, :location, :distanceMeters, true) " +
           "AND r.created_at >= :since " +
           "AND r.is_duplicate = false " +
           "ORDER BY r.created_at DESC",
           nativeQuery = true)
    List<Report> findProximateReports(
        @Param("location") Point location,
        @Param("distanceMeters") double distanceMeters,
        @Param("since") LocalDateTime since
    );

    /**
     * Find nearby reports within time window and same category
     * Used for deduplication BEFORE saving the report (no ID to exclude)
     * @param location the reference point
     * @param distanceMeters the distance threshold in meters
     * @param since the time threshold
     * @param category the report category
     * @return list of nearby reports
     */
    @Query(value = "SELECT r.* FROM reportes r WHERE " +
           "ST_DWithin(r.location, :location, :distanceMeters, true) " +
           "AND r.created_at >= :since " +
           "AND r.category = :category " +
           "AND r.is_duplicate = false " +
           "ORDER BY r.created_at DESC",
           nativeQuery = true)
    List<Report> findNearbyReportsWithinTimeWindowNoExclude(
        @Param("location") Point location,
        @Param("distanceMeters") double distanceMeters,
        @Param("since") LocalDateTime since,
        @Param("category") String category
    );

    /**
     * Find nearby reports within time window and same category
     * Used for deduplication
     * @param location the reference point
     * @param distanceMeters the distance threshold in meters
     * @param since the time threshold
     * @param category the report category
     * @param excludeReportId the ID of the report to exclude (usually the current report)
     * @return list of nearby reports
     */
    @Query(value = "SELECT r.* FROM reportes r WHERE " +
           "ST_DWithin(r.location, :location, :distanceMeters, true) " +
           "AND r.created_at >= :since " +
           "AND r.category = :category " +
           "AND r.is_duplicate = false " +
           "AND r.id != :excludeReportId " +
           "ORDER BY r.created_at DESC",
           nativeQuery = true)
    List<Report> findNearbyReportsWithinTimeWindow(
        @Param("location") Point location,
        @Param("distanceMeters") double distanceMeters,
        @Param("since") LocalDateTime since,
        @Param("category") String category,
        @Param("excludeReportId") UUID excludeReportId
    );

    /**
     * Count reports by parent task
     * @param parentTask the parent task
     * @return number of duplicate reports
     */
    int countByParentTask(Task parentTask);

    /**
     * Find non-duplicate reports
     * @return list of reports that are not marked as duplicates
     */
    List<Report> findByIsDuplicateFalse();

    /**
     * Count reports by category
     * @param category the category to count
     * @return number of reports in the category
     */
    long countByCategory(String category);
}
