package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.enums.RecurrencePattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for recurring booking creation
 *
 * Provides detailed breakdown of:
 * - Successfully created bookings
 * - Skipped bookings (due to conflicts)
 * - Conflict details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBookingResponse {
    private UUID recurringSeriesId;
    private RecurrencePattern pattern;
    private String timezone;
    private Integer totalRequested;
    private Integer successfullyCreated;
    private Integer skippedDueToConflicts;
    private Integer failedDueToErrors;
    private List<BookingResponse> createdBookings;

    private List<ConflictDetail> conflicts;
    private String message;
    private Boolean isPartialSuccess;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictDetail {
        private Instant attemptedStartTime;
        private Instant attemptedEndTime;
        private Integer occurrenceNumber;
        private String conflictReason;
        private List<UUID> conflictingBookingIds;
    }
}
