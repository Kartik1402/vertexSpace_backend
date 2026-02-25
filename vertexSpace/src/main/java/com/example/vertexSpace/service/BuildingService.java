package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.request.CreateBuildingRequest;
import com.example.vertexSpace.dto.request.UpdateBuildingRequest;
import com.example.vertexSpace.dto.response.BuildingListResponse;
import com.example.vertexSpace.dto.response.BuildingResponse;

import java.util.UUID;

/**
 * Service interface for Building operations
 * SYSTEM_ADMIN only operations
 */
public interface BuildingService {

    /**
     * Create a new building
     * @param request Building creation request
     * @return Created building response
     */
    BuildingResponse createBuilding(CreateBuildingRequest request);

    /**
     * Get building by ID
     * @param buildingId Building ID
     * @return Building response
     */
    BuildingResponse getBuildingById(UUID buildingId);

    /**
     * Get all buildings
     * @param activeOnly If true, return only active buildings
     * @return List of buildings with metadata
     */
    BuildingListResponse getAllBuildings(boolean activeOnly);

    /**
     * Update building
     * @param buildingId Building ID
     * @param request Update request
     * @return Updated building response
     */
    BuildingResponse updateBuilding(UUID buildingId, UpdateBuildingRequest request);

    /**
     * Delete building (soft delete - set isActive = false)
     * @param buildingId Building ID
     */
    void deleteBuilding(UUID buildingId);

    /**
     * Get buildings by city
     * @param city City name
     * @return List of buildings in city
     */
    BuildingListResponse getBuildingsByCity(String city);

    /**
     * Get buildings by state
     * @param state State name
     * @return List of buildings in state
     */
    BuildingListResponse getBuildingsByState(String state);
}
