package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.request.CreateResourceRequest;
import com.example.vertexSpace.dto.request.ResourceFilterRequest;
import com.example.vertexSpace.dto.request.UpdateResourceRequest;
import com.example.vertexSpace.dto.response.ResourceAvailabilityResponse;
import com.example.vertexSpace.dto.response.ResourceListResponse;
import com.example.vertexSpace.dto.response.ResourceResponse;
import com.example.vertexSpace.dto.response.ResourceStatisticsResponse;
import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import com.example.vertexSpace.service.ResourceService;
import com.example.vertexSpace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller for Resource management
 * Base path: /api/v1/resources
 *
 * Access levels:
 * - SYSTEM_ADMIN: Full access
 * - DEPT_ADMIN: Can manage resources in own department
 * - USER: Can view resources
 */
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resources", description = "Resource management APIs (Rooms, Desks, Parking)")
@SecurityRequirement(name = "bearer-auth")
public class ResourceController {

    private final ResourceService resourceService;
    private final AuthService authService;
    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Create resource", description = "Create a new resource (SYSTEM_ADMIN or DEPT_ADMIN)")
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody CreateResourceRequest request,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId = authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Creating resource: {} by user {}", request.getName(), currentUserId);
        ResourceResponse response = resourceService.createResource(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID", description = "Retrieve resource details by ID")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable("id") UUID resourceId) {
        log.info("REST: Fetching resource: {}", resourceId);
        ResourceResponse response = resourceService.getResourceById(resourceId);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    @Operation(summary = "Get resources", description = "Search resources with optional filters")
    public ResponseEntity<ResourceListResponse> getResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID floorId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.info("REST: Fetching resources with filters - type: {}, building: {}, available: {}",
                type, buildingId, available);

        ResourceFilterRequest filterRequest = new ResourceFilterRequest(
                type, buildingId, floorId, departmentId, minCapacity, available, startTime, endTime
        );

        ResourceListResponse response = resourceService.getResources(filterRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get resources by type", description = "Retrieve resources of specific type (ROOM, DESK, PARKING)")
    public ResponseEntity<ResourceListResponse> getResourcesByType(@PathVariable("type") ResourceType type) {
        log.info("REST: Fetching resources by type: {}", type);
        ResourceListResponse response = resourceService.getResourcesByType(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get resources by department", description = "Retrieve resources owned by specific department")
    public ResponseEntity<ResourceListResponse> getResourcesByDepartment(
            @PathVariable("departmentId") UUID departmentId) {
        log.info("REST: Fetching resources for department: {}", departmentId);
        ResourceListResponse response = resourceService.getResourcesByDepartment(departmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check resource availability
     * GET /api/v1/resources/{id}/availability?startTime=xxx&endTime=xxx
     * Role: Authenticated users
     */
    @GetMapping("/{id}/availability")
    @Operation(summary = "Check availability", description = "Check if resource is available in time range")
    public ResponseEntity<ResourceAvailabilityResponse> checkAvailability(
            @PathVariable("id") UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        log.info("REST: Checking availability for resource {} from {} to {}", resourceId, startTime, endTime);
        ResourceAvailabilityResponse response = resourceService.checkAvailability(resourceId, startTime, endTime);
        return ResponseEntity.ok(response);
    }

  @GetMapping("/{id}/statistics")
    @Operation(summary = "Get resource statistics", description = "Get usage statistics for resource")
    public ResponseEntity<ResourceStatisticsResponse> getResourceStatistics(@PathVariable("id") UUID resourceId) {
        log.info("REST: Fetching statistics for resource: {}", resourceId);
        ResourceStatisticsResponse response = resourceService.getResourceStatistics(resourceId);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Update resource", description = "Update resource details (SYSTEM_ADMIN or DEPT_ADMIN)")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable("id") UUID resourceId,
            @Valid @RequestBody UpdateResourceRequest request,
            Authentication authentication) {
        String currentUserEmail = authentication.getName();
        UUID currentUserId = authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Updating resource: {} by user {}", resourceId, currentUserId);
        ResourceResponse response = resourceService.updateResource(resourceId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Delete resource", description = "Soft delete resource (SYSTEM_ADMIN or DEPT_ADMIN)")
    public ResponseEntity<Void> deleteResource(
            @PathVariable("id") UUID resourceId,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId = authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Deleting resource: {} by user {}", resourceId, currentUserId);
        resourceService.deleteResource(resourceId, currentUserId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{resourceId}/assignment-mode")
    @Operation(
            summary = "Change desk assignment mode (System Admin only)",
            description = "Convert a desk between ASSIGNED and HOT_DESK modes. " +
                    "WARNING: Changing mode may affect existing bookings/assignments."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment mode updated"),
            @ApiResponse(responseCode = "403", description = "Only System Admin can change modes"),
            @ApiResponse(responseCode = "400", description = "Invalid mode for this resource type")
    })
    public ResponseEntity<ResourceResponse> changeAssignmentMode(
            @PathVariable UUID resourceId,
            @RequestParam AssignmentMode newMode,
            Authentication authentication
    ) {
        String currentUserEmail = authentication.getName();
        UUID currentUserId = authService.getUserIdByEmail(currentUserEmail);
        ResourceResponse updated = resourceService.changeAssignmentMode(resourceId, newMode, currentUserId);
        return ResponseEntity.ok(updated);
    }

}
