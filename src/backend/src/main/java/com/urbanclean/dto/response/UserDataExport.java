package com.urbanclean.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for complete user data export (GDPR compliance)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDataExport {
    
    private UserProfileExport profile;
    private List<ReportExport> reports;
    private List<FeedbackExport> feedback;
    private ExportMetadata metadata;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserProfileExport {
        private String userId;
        private String username;
        private String email;
        private String role;
        private String createdAt; // ISO 8601 format
        private String updatedAt; // ISO 8601 format
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportExport {
        private String reportId;
        private Double latitude; // WGS84
        private Double longitude; // WGS84
        private String category;
        private String description;
        private String photoUrl;
        private String createdAt; // ISO 8601 format
        private Boolean isDuplicate;
        private String taskId;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackExport {
        private String feedbackId;
        private String taskId;
        private String type;
        private String justification;
        private String submittedAt; // ISO 8601 format
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportMetadata {
        private String exportedAt; // ISO 8601 format
        private String dataFormat;
        private String version;
        private Integer totalReports;
        private Integer totalFeedback;
    }
}
