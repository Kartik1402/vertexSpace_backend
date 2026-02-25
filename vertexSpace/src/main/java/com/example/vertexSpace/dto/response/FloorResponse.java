package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for floor details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorResponse {

    private UUID id;
    private UUID buildingId;
    private String buildingName;
    private Integer floorNumber;
    private String floorName;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional: Count of resources (if needed)
    private Long resourceCount;
}
