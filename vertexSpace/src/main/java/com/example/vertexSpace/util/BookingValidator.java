package com.example.vertexSpace.util;

import com.example.vertexSpace.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class BookingValidator {

    private static final long MAX_BOOKING_DURATION_HOURS = 8;
    private static final long MAX_ADVANCE_BOOKING_DAYS = 90;
    private static final long MIN_BOOKING_DURATION_MINUTES = 15;

    public void validateBookingTime(Instant startTime, Instant endTime) {
        Instant now = Instant.now();

        // Check if start time is in the past
        if (startTime.isBefore(now)) {
            throw new ValidationException("Start time cannot be in the past");
        }

        // Check if end time is after start time
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("End time must be after start time");
        }

        // Check minimum duration (15 minutes)
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < MIN_BOOKING_DURATION_MINUTES) {
            throw new ValidationException(
                    String.format("Booking duration must be at least %d minutes", MIN_BOOKING_DURATION_MINUTES)
            );
        }

        // Check maximum duration (8 hours)
        long durationHours = Duration.between(startTime, endTime).toHours();
        if (durationHours > MAX_BOOKING_DURATION_HOURS) {
            throw new ValidationException(
                    String.format("Booking duration cannot exceed %d hours", MAX_BOOKING_DURATION_HOURS)
            );
        }

        // Check maximum advance booking (90 days)
        long daysInAdvance = Duration.between(now, startTime).toDays();
        if (daysInAdvance > MAX_ADVANCE_BOOKING_DAYS) {
            throw new ValidationException(
                    String.format("Cannot book more than %d days in advance", MAX_ADVANCE_BOOKING_DAYS)
            );
        }
    }

    /**
     * Validate buffer minutes
     */
    public void validateBufferMinutes(Integer bufferMinutes) {
        if (bufferMinutes == null) {
            return;  // Allow null (will use default)
        }

        if (bufferMinutes < 0) {
            throw new ValidationException("Buffer minutes cannot be negative");
        }

        if (bufferMinutes > 60) {
            throw new ValidationException("Buffer minutes cannot exceed 60");
        }
    }

    public boolean isSameDay(Instant time1, Instant time2) {
        ZonedDateTime zdt1 = time1.atZone(ZoneId.systemDefault());
        ZonedDateTime zdt2 = time2.atZone(ZoneId.systemDefault());

        return zdt1.toLocalDate().equals(zdt2.toLocalDate());
    }

    public Instant calculateConflictEndTime(Instant endTime, Integer bufferMinutes) {
        if (bufferMinutes == null || bufferMinutes == 0) {
            return endTime;
        }
        return endTime.plusSeconds(bufferMinutes * 60L);
    }

    public String formatDuration(Instant startTime, Instant endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d hour%s %d minute%s",
                    hours, hours > 1 ? "s" : "",
                    remainingMinutes, remainingMinutes != 1 ? "s" : "");
        } else {
            return String.format("%d minute%s", minutes, minutes != 1 ? "s" : "");
        }
    }
}
