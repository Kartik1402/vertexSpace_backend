package com.example.vertexSpace.dto.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for dashboard recommendations
 *
 * GET /api/v1/me/recommendations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Personalized resource recommendations (top 3 most-booked)")
public class RecommendationResponseDTO {

    @Schema(description = "Top 3 resources user books most frequently")
    private List<RecommendationItemDTO> recommendations;

    @Schema(description = "Analysis period", example = "Last 30 days")
    private String period;
}
