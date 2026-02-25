package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.request.CreateFloorRequest;
import com.example.vertexSpace.dto.request.UpdateFloorRequest;
import com.example.vertexSpace.dto.response.FloorListResponse;
import com.example.vertexSpace.dto.response.FloorResponse;

import java.util.UUID;

/**
 * Service interface for Floor operations
 * SYSTEM_ADMIN only operations
 */
public interface FloorService {

    /**
     * Create a new floor
     * @param request Floor creation request
     * @return Created floor response
     */
    FloorResponse createFloor(CreateFloorRequest request);

    /**
     * Get floor by ID
     * @param floorId Floor ID
     * @return Floor response
     */
    FloorResponse getFloorById(UUID floorId);

    /**
     * Get all floors
     * @param activeOnly If true, return only active floors
     * @return List of floors with metadata
     */
    FloorListResponse getAllFloors(boolean activeOnly);

    /**
     * Get floors by building ID
     * @param buildingId Building ID
     * @param activeOnly If true, return only active floors
     * @return List of floors in building
     */
    FloorListResponse getFloorsByBuildingId(UUID buildingId, boolean activeOnly);

    /**
     * Update floor
     * @param floorId Floor ID
     * @param request Update request
     * @return Updated floor response
     */
    FloorResponse updateFloor(UUID floorId, UpdateFloorRequest request);

    /**
     * Delete floor (soft delete - set isActive = false)
     * @param floorId Floor ID
     */
    void deleteFloor(UUID floorId);
}
