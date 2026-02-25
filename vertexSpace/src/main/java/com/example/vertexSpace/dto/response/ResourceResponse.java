package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for resource details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {

    private UUID id;

    // Floor details
    private UUID floorId;
    private String floorName;
    private Integer floorNumber;

    // Building details
    private UUID buildingId;
    private String buildingName;

    // Department details
    private UUID owningDepartmentId;
    private String owningDepartmentName;
    private String owningDepartmentCode;
    private AssignmentMode assignmentMode;


    // Resource details
    private ResourceType resourceType;
    private String name;
    private Integer capacity;  // Null for DESK and PARKING
    private String description;
    private Boolean isActive;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isAssignableDesk;
    private Boolean isHotDesk;
    private Boolean isBookable;

    private Instant createdAtUtc;
    private Instant updatedAtUtc;
}
