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

    /**
     * Unique ID for this recurring series
     * All bookings in the series share this ID
     */
    private UUID recurringSeriesId;

    /**
     * Recurrence pattern used
     */
    private RecurrencePattern pattern;

    /**
     * Original timezone
     */
    private String timezone;

    // ========================================================================
    // RESULTS SUMMARY
    // ========================================================================

    /**
     * Total number of occurrences requested
     * Example: 12 (for "every Monday for 12 weeks")
     */
    private Integer totalRequested;

    /**
     * Number of bookings successfully created
     * Example: 10 (if 2 had conflicts)
     */
    private Integer successfullyCreated;

    /**
     * Number of occurrences skipped due to conflicts
     * Example: 2 (if 2 dates were already booked)
     */
    private Integer skippedDueToConflicts;

    /**
     * Number of occurrences that failed due to errors
     * Example: 0 (usually zero unless system error)
     */
    private Integer failedDueToErrors;

    // ========================================================================
    // DETAILED RESULTS
    // ========================================================================

    /**
     * List of successfully created bookings
     * Each entry has full booking details (ID, times, resource, etc.)
     */
    private List<BookingResponse> createdBookings;

    /**
     * List of conflicts encountered
     * Shows which dates were skipped and why
     */
    private List<ConflictDetail> conflicts;

    // ========================================================================
    // USER-FRIENDLY MESSAGE
    // ========================================================================

    /**
     * Summary message for user
     * Example: "Successfully created 10 out of 12 bookings. 2 dates skipped due to conflicts."
     */
    private String message;

    /**
     * True if some (but not all) occurrences were created
     * False if all succeeded or all failed
     */
    private Boolean isPartialSuccess;

    // ========================================================================
    // NESTED CLASSES
    // ========================================================================

    /**
     * Details about a single conflict
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictDetail {

        /**
         * When we tried to book (UTC)
         */
        private Instant attemptedStartTime;

        /**
         * When the booking would have ended (UTC)
         */
        private Instant attemptedEndTime;

        /**
         * Which occurrence this was (1st, 2nd, 3rd, etc.)
         */
        private Integer occurrenceNumber;

        /**
         * Human-readable reason
         * Example: "Resource already booked" or "You have another booking at this time"
         */
        private String conflictReason;

        /**
         * IDs of conflicting bookings (if applicable)
         */
        private List<UUID> conflictingBookingIds;
    }
}
