package com.example.vertexSpace.dto.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for best-slot suggestions
 *
 * GET /api/v1/availability/best-slots
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Best available slot suggestions (top 5)")
public class BestSlotResponseDTO {

    @Schema(description = "Search date (IST local date)")
    private LocalDate dateIst;

    @Schema(description = "Search window", example = "08:00-20:00 IST")
    private String searchWindow;

    @Schema(description = "Requested duration (minutes)")
    private Integer durationMinutes;

    @Schema(description = "Number of suggestions found", example = "5")
    private Integer totalSuggestions;

    @Schema(description = "Top 5 available slots (earliest first)")
    private List<SlotSuggestionDTO> suggestions;
}
