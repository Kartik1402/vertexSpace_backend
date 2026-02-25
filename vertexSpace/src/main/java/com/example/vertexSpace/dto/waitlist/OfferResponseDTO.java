package com.example.vertexSpace.dto.waitlist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for waitlist offers
 *
 * GET /api/v1/me/waitlist-offers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Active waitlist offer waiting for user response")
public class OfferResponseDTO {

    @Schema(description = "Offer ID (time block ID)")
    private UUID id;

    @Schema(description = "Resource ID")
    private UUID resourceId;

    @Schema(description = "Resource name")
    private String resourceName;

    @Schema(description = "Resource type", example = "ROOM")
    private String resourceType;

    @Schema(description = "Start time (UTC)")
    private Instant startUtc;

    @Schema(description = "End time (UTC)")
    private Instant endUtc;

    @Schema(description = "Offer status", example = "OFFERED")
    private String status;

    @Schema(description = "When offer was created")
    private Instant offeredAt;

    @Schema(description = "When offer expires (10 min from offeredAt)")
    private Instant expiresAt;

    @Schema(description = "Seconds remaining until expiry", example = "485")
    private Long remainingSeconds;

    @Schema(description = "Original waitlist entry ID")
    private UUID waitlistEntryId;
}
