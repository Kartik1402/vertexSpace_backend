package com.example.vertexSpace.dto.availability;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request parameters for best-slot suggestions")
public class BestSlotRequestDTO {

    @NotNull(message = "Date is required")
    @Schema(description = "Target date in IST (local date, no timezone)", example = "2025-01-15")
    private LocalDate dateIst;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Minimum duration is 15 minutes")
    @Max(value = 480, message = "Maximum duration is 8 hours")
    @Schema(description = "How long you need (minutes)", example = "60")
    private Integer durationMinutes;

    @Schema(description = "Filter by resource type", example = "ROOM")
    private String type;

    @Schema(description = "Filter by floor ID")
    private UUID floorId;

    @Schema(description = "Filter by minimum capacity", example = "10")
    private Integer capacityMin;

    @Schema(description = "Filter by building ID")
    private UUID buildingId;

    @Schema(description = "Filter by department ID (prefer own dept)")
    private UUID departmentId;

    @Schema(description = "Filter by required features (IDs)")
    private List<UUID> featureIds;


}
