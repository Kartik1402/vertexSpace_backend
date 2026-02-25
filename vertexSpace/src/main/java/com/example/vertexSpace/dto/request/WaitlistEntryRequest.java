package com.example.vertexSpace.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating a waitlist entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntryRequest {

    private UUID resourceId;

    private Instant requestedStartTime;

    private Instant requestedEndTime;

    private String purpose;

    private Integer bufferMinutes;
}
