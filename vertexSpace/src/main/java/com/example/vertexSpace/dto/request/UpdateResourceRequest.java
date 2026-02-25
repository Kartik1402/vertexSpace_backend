package com.example.vertexSpace.dto.request;

import com.example.vertexSpace.enums.ResourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating a resource
 * All fields are optional
 * SYSTEM_ADMIN: can update any resource
 * DEPT_ADMIN: can only update resources in own department
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResourceRequest {

    private UUID floorId;

    private UUID owningDepartmentId;

    private ResourceType resourceType;

    @Size(max = 100, message = "Resource name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000, message = "Capacity must not exceed 1000")
    private Integer capacity;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive;
}
