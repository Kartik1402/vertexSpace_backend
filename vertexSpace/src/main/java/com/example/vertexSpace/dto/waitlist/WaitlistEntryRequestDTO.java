package com.example.vertexSpace.dto.waitlist;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for manually joining waitlist
 *
 * Used when user explicitly wants to join waitlist without
 * attempting to book first. Most users will join automatically
 * when booking attempt fails due to conflict.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntryRequestDTO {

    /**
     * Resource ID to wait for
     */
    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    /**
     * Desired start time (must be in the future)
     */
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant startUtc;

    /**
     * Desired end time
     */
    @NotNull(message = "End time is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant endUtc;

    /**
     * Purpose/reason for the booking (optional)
     */
    private String purpose;

    /**
     * Buffer minutes (optional, defaults to 15)
     */
    private Integer bufferMinutes;
}
