package com.example.vertexSpace.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFloorRequest {

    private UUID buildingId;

    private Integer floorNumber;

    @Size(max = 100, message = "Floor name must not exceed 100 characters")
    private String floorName;

    private Boolean isActive;
}
