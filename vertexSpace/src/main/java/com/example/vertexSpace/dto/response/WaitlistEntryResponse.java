package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.enums.WaitlistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for waitlist entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntryResponse {

    private UUID id;
    private UUID resourceId;
    private String resourceName;
    private UUID userId;
    private String userDisplayName;
    private UUID pendingBookingId;  // The PENDING_WAITLIST booking
    private Instant requestedStartTime;
    private Instant requestedEndTime;
    private String purpose;
    private WaitlistStatus status;
    private Integer queuePosition;
    private Instant createdAt;
    private String message;
}
