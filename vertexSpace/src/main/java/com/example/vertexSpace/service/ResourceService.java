package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.request.CreateResourceRequest;
import com.example.vertexSpace.dto.request.ResourceFilterRequest;
import com.example.vertexSpace.dto.request.UpdateResourceRequest;
import com.example.vertexSpace.dto.response.ResourceAvailabilityResponse;
import com.example.vertexSpace.dto.response.ResourceListResponse;
import com.example.vertexSpace.dto.response.ResourceResponse;
import com.example.vertexSpace.dto.response.ResourceStatisticsResponse;
import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for Resource operations
 * SYSTEM_ADMIN: Full access
 * DEPT_ADMIN: Can manage resources in own department
 */
public interface ResourceService {

    /**
     * Create a new resource
     * @param request Resource creation request
     * @param currentUserId Current user's ID (for authorization)
     * @return Created resource response
     */
    ResourceResponse createResource(CreateResourceRequest request, UUID currentUserId);

    /**
     * Get resource by ID
     * @param resourceId Resource ID
     * @return Resource response
     */
    ResourceResponse getResourceById(UUID resourceId);

    /**
     * Get all resources with optional filters
     * @param filterRequest Filter criteria (all optional)
     * @return Filtered list of resources with metadata
     */
    ResourceListResponse getResources(ResourceFilterRequest filterRequest);

    /**
     * Get resources by type
     * @param type Resource type (ROOM, DESK, PARKING)
     * @return List of resources of specified type
     */
    ResourceListResponse getResourcesByType(ResourceType type);

    /**
     * Get resources by department
     * @param departmentId Department ID
     * @return List of resources owned by department
     */
    ResourceListResponse getResourcesByDepartment(UUID departmentId);

    /**
     * Update resource
     * @param resourceId Resource ID
     * @param request Update request
     * @param currentUserId Current user's ID (for authorization)
     * @return Updated resource response
     */
    ResourceResponse updateResource(UUID resourceId, UpdateResourceRequest request, UUID currentUserId);

    /**
     * Delete resource (soft delete - set isActive = false)
     * @param resourceId Resource ID
     * @param currentUserId Current user's ID (for authorization)
     */
    void deleteResource(UUID resourceId, UUID currentUserId);

    /**
     * Check resource availability in time range
     * @param resourceId Resource ID
     * @param startTime Start time
     * @param endTime End time
     * @return Availability response with conflict details
     */
    ResourceAvailabilityResponse checkAvailability(UUID resourceId, Instant startTime, Instant endTime);

    /**
     * Get resource usage statistics
     * @param resourceId Resource ID
     * @return Statistics response
     */
    ResourceStatisticsResponse getResourceStatistics(UUID resourceId);
    ResourceResponse changeAssignmentMode(UUID resourceId, AssignmentMode newMode, UUID currentUserId);
}
