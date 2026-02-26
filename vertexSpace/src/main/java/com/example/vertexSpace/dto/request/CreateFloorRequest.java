package com.example.vertexSpace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFloorRequest {

    @NotNull(message = "Building ID is required")
    private UUID buildingId;

    @NotNull(message = "Floor number is required")
    private Integer floorNumber;  // Can be negative for basements (e.g., -1, -2)

    @NotBlank(message = "Floor name is required")
    @Size(max = 100, message = "Floor name must not exceed 100 characters")
    private String floorName;  // e.g., "Ground Floor", "1st Floor", "Basement Level 1"
}
