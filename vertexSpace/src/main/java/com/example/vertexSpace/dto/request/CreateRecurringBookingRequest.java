package com.example.vertexSpace.dto.request;

import com.example.vertexSpace.enums.ConflictResolution;
import com.example.vertexSpace.enums.RecurrenceEndType;
import com.example.vertexSpace.enums.RecurrencePattern;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecurringBookingRequest {

    // ========================================================================
    // BASIC BOOKING INFO (same as single booking)
    // ========================================================================

    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    /**
     * Start time of FIRST occurrence (in user's timezone)
     * Example: "2026-03-03T14:00:00" for 2 PM IST on March 3
     *
     * System will convert to UTC and apply pattern for subsequent occurrences
     */
    @NotNull(message = "Start time is required")
    private Instant startTime;

    /**
     * End time of FIRST occurrence (in user's timezone)
     * Example: "2026-03-03T16:00:00" for 4 PM IST on March 3
     */
    @NotNull(message = "End time is required")
    private Instant endTime;

    @Min(value = 0, message = "Buffer minutes cannot be negative")
    @Max(value = 60, message = "Buffer minutes cannot exceed 60")
    private Integer bufferMinutes = 15;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;

    // ========================================================================
    // RECURRENCE PATTERN
    // ========================================================================

    /**
     * How often to repeat
     * WEEKLY = every week on specified days
     * DAILY = every day
     * MONTHLY = every month on specified day
     */
    @NotNull(message = "Recurrence pattern is required")
    private RecurrencePattern pattern;

    /**
     * For WEEKLY pattern: which days of week
     * Example: [MONDAY, WEDNESDAY, FRIDAY]
     * Required if pattern = WEEKLY
     */
    private List<DayOfWeek> daysOfWeek;

    /**
     * For MONTHLY pattern: which day of month (1-31)
     * Example: 15 for "15th of every month"
     * Required if pattern = MONTHLY
     */
    @Min(value = 1, message = "Day of month must be between 1 and 31")
    @Max(value = 31, message = "Day of month must be between 1 and 31")
    private Integer dayOfMonth;

    // ========================================================================
    // END CONDITION (when to stop creating occurrences)
    // ========================================================================

    /**
     * How to determine when to stop
     * END_DATE = stop after specific date
     * OCCURRENCES = stop after N occurrences (RECOMMENDED)
     * NEVER = indefinite (max 52 weeks)
     */
    @NotNull(message = "End type is required")
    private RecurrenceEndType endType;

    /**
     * If endType = END_DATE: when to stop
     * Example: "2026-06-30T23:59:59" for "until end of June"
     */
    private Instant recurrenceEndDate;

    /**
     * If endType = OCCURRENCES: how many times to repeat
     * Example: 12 for "12 weeks"
     * RECOMMENDED: Most predictable for users
     */
    @Min(value = 1, message = "Must have at least 1 occurrence")
    @Max(value = 52, message = "Cannot exceed 52 occurrences (1 year)")
    private Integer numberOfOccurrences;

    // ========================================================================
    // TIMEZONE & CONFLICT HANDLING
    // ========================================================================

    /**
     * User's timezone (IANA timezone ID)
     * Example: "Asia/Kolkata" for IST
     * Defaults to IST if not specified
     */
    @NotNull(message = "Timezone is required")
    private String timezone = "Asia/Kolkata";

    /**
     * How to handle conflicts
     * SKIP_CONFLICTS = create available, skip conflicts (RECOMMENDED)
     * FAIL_ON_CONFLICT = reject entire series if any conflict
     * INTERACTIVE = not yet implemented
     */
    @NotNull(message = "Conflict resolution strategy is required")
    private ConflictResolution conflictResolution = ConflictResolution.SKIP_CONFLICTS;

    // ========================================================================
    // VALIDATION METHODS
    // ========================================================================

    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    @AssertTrue(message = "Booking duration cannot exceed 8 hours")
    public boolean isReasonableDuration() {
        if (startTime == null || endTime == null) {
            return true;
        }
        long durationMinutes = (endTime.getEpochSecond() - startTime.getEpochSecond()) / 60;
        return durationMinutes <= 480;
    }

    @AssertTrue(message = "Must specify days of week for WEEKLY pattern")
    public boolean isDaysOfWeekValid() {
        if (pattern == RecurrencePattern.WEEKLY) {
            return daysOfWeek != null && !daysOfWeek.isEmpty() && daysOfWeek.size() <= 7;
        }
        return true;
    }

    @AssertTrue(message = "Must specify day of month for MONTHLY pattern")
    public boolean isDayOfMonthValid() {
        if (pattern == RecurrencePattern.MONTHLY) {
            return dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31;
        }
        return true;
    }

    @AssertTrue(message = "Must specify end date when using END_DATE type")
    public boolean isEndDateValid() {
        if (endType == RecurrenceEndType.END_DATE) {
            return recurrenceEndDate != null && recurrenceEndDate.isAfter(startTime);
        }
        return true;
    }

    @AssertTrue(message = "Must specify number of occurrences when using OCCURRENCES type")
    public boolean isOccurrencesValid() {
        if (endType == RecurrenceEndType.OCCURRENCES) {
            return numberOfOccurrences != null && numberOfOccurrences > 0;
        }
        return true;
    }

    @AssertTrue(message = "Start time cannot be in the past")
    public boolean isStartTimeNotInPast() {
        if (startTime == null) {
            return true;
        }
        return startTime.isAfter(Instant.now());
    }
}
