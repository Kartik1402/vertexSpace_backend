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

    /**
     * The requested time slot
     */
    private Instant requestedStartTimeUtc;
    private Instant requestedEndTimeUtc;

    /**
     * Conflicting bookings information
     */
    private List<ConflictingBooking> conflicts;

    /**
     * Waitlist information
     */
    private WaitlistInfo waitlistInfo;

    /**
     * Alternative available resources
     */
    private List<ResourceResponse> alternativeResources;

    /**
     * Suggested alternative time slots
     */
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
        // ❌ REMOVED: estimatedWaitTime - we can't accurately predict cancellations
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotSuggestion {
        private Instant startTimeUtc;
        private Instant endTimeUtc;
        private String availability;  // "Available now", "Available tomorrow"
    }
}
