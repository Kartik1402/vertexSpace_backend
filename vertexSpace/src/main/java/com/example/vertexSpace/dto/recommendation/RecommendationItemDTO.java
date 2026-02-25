package com.example.vertexSpace.dto.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Single recommendation item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resource recommendation based on booking history")
public class RecommendationItemDTO {

    @Schema(description = "Resource ID")
    private UUID resourceId;

    @Schema(description = "Resource name")
    private String resourceName;

    @Schema(description = "Resource type", example = "ROOM")
    private String resourceType;

    @Schema(description = "How many times user booked (last 30 days)", example = "15")
    private Long bookingCount;

    @Schema(description = "When user last booked this resource")
    private Instant lastBookedAt;

    @Schema(description = "Floor name")
    private String floorName;

    @Schema(description = "Building name")
    private String buildingName;
}
