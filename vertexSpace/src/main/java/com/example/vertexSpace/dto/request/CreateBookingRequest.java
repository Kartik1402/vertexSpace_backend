package com.example.vertexSpace.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @Min(value = 0, message = "Buffer minutes cannot be negative")
    @Max(value = 60, message = "Buffer minutes cannot exceed 60")
    private Integer bufferMinutes = 15;  // Default 15 minutes

    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    private String purpose;

    /**
     * Custom validation: End time must be after start time
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) {
            return true;  // Let @NotNull handle null validation
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Custom validation: Booking duration should be reasonable (max 8 hours)
     */
    @AssertTrue(message = "Booking duration cannot exceed 8 hours")
    public boolean isReasonableDuration() {
        if (startTime == null || endTime == null) {
            return true;
        }
        long durationMinutes = (endTime.getEpochSecond() - startTime.getEpochSecond()) / 60;
        return durationMinutes <= 480;  // 8 hours = 480 minutes
    }
}
