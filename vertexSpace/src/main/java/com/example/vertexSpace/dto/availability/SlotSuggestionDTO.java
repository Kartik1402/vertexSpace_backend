package com.example.vertexSpace.dto.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Available time slot suggestion")
public class SlotSuggestionDTO {

    @Schema(description = "Resource ID")
    private UUID resourceId;

    @Schema(description = "Resource name")
    private String resourceName;

    @Schema(description = "Resource type", example = "ROOM")
    private String resourceType;

    @Schema(description = "Start time (UTC for backend)")
    private Instant startUtc;

    @Schema(description = "End time (UTC for backend)")
    private Instant endUtc;

    @Schema(description = "Start time (IST for display)")
    private ZonedDateTime startIst;

    @Schema(description = "End time (IST for display)")
    private ZonedDateTime endIst;

    @Schema(description = "Capacity (for rooms)")
    private Integer capacity;

    @Schema(description = "Floor name")
    private String floorName;

    @Schema(description = "Building name")
    private String buildingName;
}
