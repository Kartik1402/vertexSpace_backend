package com.example.vertexSpace.dto.request;

import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceRequest {

    @NotNull(message = "Floor ID is required")
    private UUID floorId;

    @NotNull(message = "Owning department ID is required")
    private UUID owningDepartmentId;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;
    private AssignmentMode assignmentMode;


    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000, message = "Capacity must not exceed 1000")
    private Integer capacity;  // Required for ROOM, should be null for DESK/PARKING

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Custom validation: Capacity is required for ROOM type
     */
    @AssertTrue(message = "Capacity is required for ROOM type resources")
    public boolean isCapacityValidForType() {
        if (resourceType == ResourceType.ROOM) {
            return capacity != null && capacity > 0;
        }
        // For DESK and PARKING, capacity should be null or will be ignored
        return true;
    }
}
