package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceAvailabilityResponse {

    private UUID resourceId;
    private String resourceName;
    private Boolean isAvailable;
    private Instant requestedStartTime;
    private Instant requestedEndTime;

    // If not available, list conflicting bookings
    private List<ConflictingBooking> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictingBooking {
        private UUID bookingId;
        private Instant startTime;
        private Instant endTime;
        private Instant conflictEndTime;
        private String bookedByUser;
    }
}
