# Requirements Document

## Introduction

This document specifies notification system, analytics dashboard, user profile management, and system configuration requirements for the Urban Cleaning Management System. These requirements enhance user experience, provide operational insights, and enable comprehensive system administration.

## Glossary

- **System**: The Urban Cleaning Management WebApp
- **Citizen**: A user with ROLE_CIUDADANO who can report cleaning incidents
- **Operator**: A user with ROLE_TECNICO who manages and resolves cleaning tasks
- **Administrator**: A user with ROLE_ADMIN who configures system parameters
- **Task**: A work item created from one or more reports, assigned to operators
- **Email_Notification**: Asynchronous email alerts for system events
- **Notification_Preference**: User settings controlling which notifications they receive
- **MTTR**: Mean Time To Resolution - average time to resolve tasks
- **Heatmap**: Geographic visualization showing incident concentration
- **KPI**: Key Performance Indicator - metrics for operational decision-making
- **System_Config**: Runtime-configurable system parameters
- **SMTP**: Simple Mail Transfer Protocol for email delivery
- **Event_Driven**: Architecture pattern where actions trigger asynchronous events

## Requirements

### Requirement 1: Email Notification System

**User Story:** As a system user, I want to receive email notifications about important events, so that I stay informed about my reports and tasks.

#### Acceptance Criteria

1. WHEN a task state changes to RESUELTO, THE System SHALL send an email notification to the citizen reporter
2. WHEN a task is reopened (REABIERTO), THE System SHALL send an email notification to the assigned operator
3. WHEN a new task is assigned to an operator, THE System SHALL send an email notification to that operator
4. THE System SHALL process email sending asynchronously using Spring Events or message queue to avoid blocking API responses
5. WHEN email sending fails, THE System SHALL log the failure with error details and retry up to 3 times
6. THE System SHALL use exponential backoff for email retry attempts (1 minute, 5 minutes, 15 minutes)
7. THE System SHALL use HTML templates for email formatting with responsive design
8. THE System SHALL allow users to enable or disable notification preferences per notification type
9. WHEN an email cannot be delivered after all retries, THE System SHALL record the failure in a notification_failures table for administrator review
10. THE System SHALL include unsubscribe links in all notification emails
11. WHEN a user clicks unsubscribe, THE System SHALL disable that notification type for the user
12. THE System SHALL support email templates with variables: {userName}, {taskId}, {category}, {location}, {priority}
13. THE System SHALL validate email addresses before attempting to send notifications
14. WHEN sending notifications, THE System SHALL respect user's notification preferences
15. THE System SHALL track email delivery status (sent, delivered, failed, bounced)

### Requirement 2: Notification Preferences Management

**User Story:** As a user, I want to control which notifications I receive, so that I only get relevant alerts.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint for users to retrieve their notification preferences
2. THE System SHALL provide an endpoint for users to update their notification preferences
3. THE System SHALL support the following notification types: TASK_ASSIGNED, TASK_RESOLVED, TASK_REOPENED, REPORT_CREATED
4. WHEN a user registers, THE System SHALL enable all notification types by default
5. THE System SHALL allow users to enable or disable each notification type independently
6. WHEN notification preferences are updated, THE System SHALL apply changes immediately
7. THE System SHALL validate that at least one notification type remains enabled
8. THE System SHALL store notification preferences in the database linked to the user account
9. THE System SHALL include notification preferences in user data exports for GDPR compliance

### Requirement 3: Analytics Dashboard - Task Distribution

**User Story:** As an Administrator, I want to view task distribution metrics, so that I can understand workload patterns.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to retrieve task distribution by category
2. THE System SHALL provide an endpoint to retrieve task distribution by state
3. WHEN retrieving task distribution, THE System SHALL return counts and percentages for each category/state
4. THE System SHALL use database aggregation functions (GROUP BY, COUNT) for efficient queries
5. THE System SHALL cache analytics results for 5 minutes using Spring Cache to reduce database load
6. THE System SHALL support filtering analytics by date range using start_date and end_date parameters
7. THE System SHALL support filtering analytics by geographic zone using zone_id parameter
8. WHEN no filters are applied, THE System SHALL return data for the last 30 days
9. THE System SHALL return results in JSON format with clear structure
10. THE System SHALL include metadata in responses: total_tasks, date_range, filters_applied

### Requirement 4: Analytics Dashboard - Performance Metrics

**User Story:** As an Administrator, I want to view performance metrics like MTTR, so that I can measure operational efficiency.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to calculate Mean Time To Resolution (MTTR)
2. WHEN calculating MTTR, THE System SHALL compute the average time between task creation and resolution
3. THE System SHALL express MTTR in hours with two decimal places
4. THE System SHALL calculate MTTR only for tasks in RESUELTO state
5. THE System SHALL support filtering MTTR by category, zone, and date range
6. THE System SHALL provide an endpoint to retrieve resolution time distribution (histogram data)
7. THE System SHALL categorize resolution times into buckets: <24h, 24-48h, 48-72h, >72h
8. THE System SHALL calculate average priority score for resolved tasks
9. THE System SHALL provide an endpoint to retrieve operator performance metrics (tasks resolved per operator)
10. THE System SHALL cache performance metrics for 5 minutes
11. WHEN loading analytics endpoints, THE System SHALL respond within 2 seconds

### Requirement 5: Analytics Dashboard - Geographic Heatmap

**User Story:** As an Administrator, I want to view a heatmap of incident concentration, so that I can identify problem areas.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to generate heatmap data showing incident concentration by geographic area
2. THE System SHALL divide the municipality into a grid of configurable cell size (default 500m x 500m)
3. WHEN generating heatmap data, THE System SHALL count reports within each grid cell
4. THE System SHALL return heatmap data as an array of objects with: latitude, longitude, intensity (count)
5. THE System SHALL use PostGIS spatial functions for efficient geographic aggregation
6. THE System SHALL support filtering heatmap by category, date range, and task state
7. THE System SHALL normalize intensity values to a 0-1 scale for visualization
8. THE System SHALL include only cells with at least one report to reduce data size
9. THE System SHALL cache heatmap data for 10 minutes due to computational cost
10. THE System SHALL support different aggregation levels: neighborhood, district, municipality
11. WHEN generating heatmap for large areas, THE System SHALL limit results to top 1000 cells by intensity

### Requirement 6: User Profile Management

**User Story:** As a user, I want to manage my personal information and view my activity history, so that I can maintain control over my data.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint for users to retrieve their own profile information
2. THE System SHALL provide an endpoint for users to update their own profile information (name, phone, preferences)
3. THE System SHALL provide an endpoint for users to change their password
4. THE System SHALL provide an endpoint for users to view their complete report history with pagination
5. WHEN a user updates profile information, THE System SHALL validate that they can only modify their own data (IDOR protection)
6. WHEN a user changes their password, THE System SHALL require the current password for verification
7. THE System SHALL validate that the new password meets complexity requirements
8. THE System SHALL validate that the new password is different from the current password
9. THE System SHALL allow users to update their email address with email verification
10. WHEN email is changed, THE System SHALL send verification emails to both old and new addresses
11. THE System SHALL not apply email change until the new email is verified
12. THE System SHALL allow users to upload a profile photo with validation (max 2MB, JPEG/PNG only)
13. THE System SHALL store profile photos separately from report photos
14. THE System SHALL provide an endpoint to retrieve user activity statistics (reports submitted, tasks resolved if operator)

### Requirement 7: System Configuration Management

**User Story:** As an Administrator, I want to configure all system parameters dynamically, so that I can tune the system without redeployment.

#### Acceptance Criteria

1. THE System SHALL provide endpoints to manage all runtime configuration parameters
2. THE System SHALL support configuration of: algorithm weights (Wc, Wz, Wt), JWT expiration time, duplicate detection radius, duplicate detection time window, geofencing boundaries, file size limits
3. WHEN configuration parameters are updated, THE System SHALL validate that values are within safe ranges
4. WHEN algorithm weights are updated, THE System SHALL trigger recalculation of Priority_Score for all pending tasks
5. WHEN JWT expiration time is updated, THE System SHALL apply the new value to newly generated tokens only
6. WHEN duplicate detection parameters are updated, THE System SHALL apply them to new reports immediately
7. THE System SHALL store configuration history with timestamps and user who made changes
8. THE System SHALL provide an endpoint to retrieve configuration change history
9. THE System SHALL provide an endpoint to rollback to a previous configuration version
10. THE System SHALL validate that configuration changes are made only by users with ROLE_ADMIN
11. THE System SHALL audit all configuration changes with IP address and timestamp
12. THE System SHALL provide default values for all configuration parameters
13. WHEN configuration is invalid, THE System SHALL reject the change and return descriptive error
14. THE System SHALL support configuration export and import for backup purposes

### Requirement 8: Advanced Report Filtering

**User Story:** As an Operator, I want advanced filtering options for reports and tasks, so that I can find relevant items quickly.

#### Acceptance Criteria

1. THE System SHALL provide an endpoint to search reports with multiple filter criteria
2. THE System SHALL support filtering by: category, date range, state, priority range, geographic zone, submitter
3. THE System SHALL support sorting by: creation date, priority score, state
4. THE System SHALL support pagination with configurable page size (default 20, max 100)
5. THE System SHALL return total count of matching results for pagination
6. THE System SHALL support combining multiple filters with AND logic
7. THE System SHALL support geographic radius search (find reports within X meters of a point)
8. THE System SHALL use database indexes for efficient filtering
9. WHEN no results match filters, THE System SHALL return empty array with 200 status code
10. THE System SHALL validate filter parameters and return 400 for invalid values
11. THE System SHALL support full-text search on report descriptions
12. THE System SHALL highlight search terms in results when full-text search is used

## Non-Functional Requirements

### Performance Requirements

1. Email notifications SHALL be sent asynchronously within 10 seconds of triggering event
2. Analytics endpoints SHALL respond within 2 seconds for typical queries
3. Heatmap generation SHALL complete within 3 seconds for municipality-wide data
4. Profile updates SHALL complete within 500 milliseconds
5. Configuration changes SHALL apply within 1 second

### Scalability Requirements

1. Email system SHALL handle up to 1000 notifications per minute
2. Analytics queries SHALL perform efficiently with up to 100,000 tasks in database
3. Heatmap generation SHALL scale to 50,000 reports
4. System SHALL support up to 10,000 concurrent users

### Reliability Requirements

1. Email failures SHALL not affect API response times or user experience
2. Analytics cache SHALL automatically refresh on expiration
3. Configuration changes SHALL be atomic (all or nothing)
4. System SHALL continue operating if email service is unavailable

### Usability Requirements

1. Email templates SHALL be mobile-responsive
2. Analytics data SHALL be returned in formats suitable for charting libraries
3. Error messages SHALL be clear and actionable
4. API responses SHALL include helpful metadata

### Maintainability Requirements

1. Email templates SHALL be stored as separate files for easy modification
2. Configuration parameters SHALL be documented with valid ranges
3. Analytics queries SHALL be optimized and indexed
4. Code SHALL follow established project standards
