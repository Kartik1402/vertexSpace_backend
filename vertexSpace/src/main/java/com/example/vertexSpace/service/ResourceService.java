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

public interface ResourceService {
    ResourceResponse createResource(CreateResourceRequest request, UUID currentUserId);

    ResourceResponse getResourceById(UUID resourceId);
    ResourceListResponse getResources(ResourceFilterRequest filterRequest);
    ResourceListResponse getResourcesByType(ResourceType type);
    ResourceListResponse getResourcesByDepartment(UUID departmentId);
    ResourceResponse updateResource(UUID resourceId, UpdateResourceRequest request, UUID currentUserId);
    void deleteResource(UUID resourceId, UUID currentUserId);
    ResourceAvailabilityResponse checkAvailability(UUID resourceId, Instant startTime, Instant endTime);
    ResourceStatisticsResponse getResourceStatistics(UUID resourceId);
    ResourceResponse changeAssignmentMode(UUID resourceId, AssignmentMode newMode, UUID currentUserId);
}
