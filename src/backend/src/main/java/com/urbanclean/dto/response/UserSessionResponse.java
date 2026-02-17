package com.urbanclean.dto.response;

import com.urbanclean.entity.UserSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user session responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionResponse {

    private UUID id;

    private String deviceType;

    private String browser;

    private String os;

    private String ipAddress;

    private String city;

    private String country;

    private LocalDateTime createdAt;

    private LocalDateTime lastActivity;

    private Boolean active;

    private Boolean current;

    /**
     * Convert UserSession entity to DTO
     */
    public static UserSessionResponse fromEntity(UserSession session, UUID currentSessionId) {
        return UserSessionResponse.builder()
                .id(session.getId())
                .deviceType(session.getDeviceType() != null ? session.getDeviceType().name() : "UNKNOWN")
                .browser(session.getBrowser())
                .os(session.getOs())
                .ipAddress(session.getIpAddress())
                .city(session.getCity())
                .country(session.getCountry())
                .createdAt(session.getCreatedAt())
                .lastActivity(session.getLastActivity())
                .active(session.getActive())
                .current(session.getId().equals(currentSessionId))
                .build();
    }
}
