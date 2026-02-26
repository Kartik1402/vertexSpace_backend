package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.dto.response.ResourceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response when booking fails due to conflict
 * Provides waitlist information and alternative suggestions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConflictResponseDTO {

    /**
     * Why the booking failed
     */
    private String reason;

    /**
     * The resource that was requested
     */
    private UUID resourceId;
    private String resourceName;
    private Instant requestedStartTimeUtc;
    private Instant requestedEndTimeUtc;
    private List<ConflictingBooking> conflicts;
    private WaitlistInfo waitlistInfo;
    private List<ResourceResponse> alternativeResources;
    private List<TimeSlotSuggestion> suggestedTimeSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictingBooking {
        private UUID bookingId;
        private Instant startTimeUtc;
        private Instant endTimeUtc;
        private String bookedBy;  // Only show if SYSTEM_ADMIN
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaitlistInfo {
        private boolean canJoinWaitlist;
        private Integer currentQueueLength;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotSuggestion {
        private Instant startTimeUtc;
        private Instant endTimeUtc;
        private String availability; 
    }
}
