package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for booking cancellation confirmation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancellationResponse {

    private UUID bookingId;
    private String message;
    private Instant cancelledAt;

    // Resource details (for user reference)
    private String resourceName;
    private Instant originalStartTime;
    private Instant originalEndTime;
}
