package com.example.vertexSpace.dto.waitlist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO when offer is accepted
 *
 * POST /api/v1/waitlist-offers/{offerId}/accept
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Result of accepting waitlist offer")
public class AcceptOfferResponseDTO {

    @Schema(description = "New booking ID")
    private UUID bookingId;

    @Schema(description = "Booking status", example = "CONFIRMED")
    private String status;

    @Schema(description = "Start time (UTC)")
    private Instant startUtc;

    @Schema(description = "End time (UTC)")
    private Instant endUtc;

    @Schema(description = "Resource name")
    private String resourceName;

    @Schema(description = "Success message")
    private String message;
}
