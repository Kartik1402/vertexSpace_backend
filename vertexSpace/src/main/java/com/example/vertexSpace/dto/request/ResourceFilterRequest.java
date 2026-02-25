package com.example.vertexSpace.dto.request;

import com.example.vertexSpace.enums.ResourceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for filtering resources
 * All fields are optional (null = not filtered by that criteria)
 * Used in GET /api/v1/resources endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceFilterRequest {

    // Filter by resource type
    private ResourceType type;

    // Filter by location
    private UUID buildingId;
    private UUID floorId;

    // Filter by ownership
    private UUID departmentId;

    // Filter by capacity (minimum capacity for rooms)
    @Min(value = 1, message = "Minimum capacity must be at least 1")
    private Integer minCapacity;

    // Filter by availability
    private Boolean available;  // If true, check availability in time range
    private Instant startTime;  // Required if available=true
    private Instant endTime;    // Required if available=true

    /**
     * Custom validation: If available=true, start and end times are required
     */
    @AssertTrue(message = "Start time and end time are required when checking availability")
    public boolean isAvailabilityFilterValid() {
        if (available != null && available) {
            return startTime != null && endTime != null;
        }
        return true;
    }

    /**
     * Custom validation: End time must be after start time (if both provided)
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        if (startTime != null && endTime != null) {
            return endTime.isAfter(startTime);
        }
        return true;
    }

    /**
     * Check if any filter is applied
     */
    public boolean hasFilters() {
        return type != null || buildingId != null || floorId != null ||
                departmentId != null || minCapacity != null ||
                (available != null && available);
    }
}
