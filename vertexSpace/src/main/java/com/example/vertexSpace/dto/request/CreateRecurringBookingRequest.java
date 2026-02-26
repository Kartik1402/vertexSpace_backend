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


    @NotNull(message = "Resource ID is required")
    private UUID resourceId;
    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @Min(value = 0, message = "Buffer minutes cannot be negative")
    @Max(value = 60, message = "Buffer minutes cannot exceed 60")
    private Integer bufferMinutes = 15;

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;


    @NotNull(message = "Recurrence pattern is required")
    private RecurrencePattern pattern;

    private List<DayOfWeek> daysOfWeek;

    @Min(value = 1, message = "Day of month must be between 1 and 31")
    @Max(value = 31, message = "Day of month must be between 1 and 31")
    private Integer dayOfMonth;

    @NotNull(message = "End type is required")
    private RecurrenceEndType endType;

    private Instant recurrenceEndDate;

    @Min(value = 1, message = "Must have at least 1 occurrence")
    @Max(value = 52, message = "Cannot exceed 52 occurrences (1 year)")
    private Integer numberOfOccurrences;


    @NotNull(message = "Timezone is required")
    private String timezone = "Asia/Kolkata";

    @NotNull(message = "Conflict resolution strategy is required")
    private ConflictResolution conflictResolution = ConflictResolution.SKIP_CONFLICTS;


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
