package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.request.CreateBuildingRequest;
import com.example.vertexSpace.dto.request.UpdateBuildingRequest;
import com.example.vertexSpace.dto.response.BuildingListResponse;
import com.example.vertexSpace.dto.response.BuildingResponse;
import com.example.vertexSpace.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Buildings", description = "Building management APIs (SYSTEM_ADMIN only)")
@SecurityRequirement(name = "bearer-auth")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create building", description = "Create a new building (SYSTEM_ADMIN only)")
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody CreateBuildingRequest request) {
        log.info("REST: Creating building: {}", request.getName());
        BuildingResponse response = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building by ID", description = "Retrieve building details by ID")
    public ResponseEntity<BuildingResponse> getBuildingById(@PathVariable("id") UUID buildingId) {
        log.info("REST: Fetching building: {}", buildingId);
        BuildingResponse response = buildingService.getBuildingById(buildingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all buildings", description = "Retrieve list of all buildings")
    public ResponseEntity<BuildingListResponse> getAllBuildings(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        log.info("REST: Fetching all buildings (activeOnly={})", activeOnly);
        BuildingListResponse response = buildingService.getAllBuildings(activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get buildings by city", description = "Retrieve buildings in specific city")
    public ResponseEntity<BuildingListResponse> getBuildingsByCity(@PathVariable("city") String city) {
        log.info("REST: Fetching buildings by city: {}", city);
        BuildingListResponse response = buildingService.getBuildingsByCity(city);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/state/{state}")
    @Operation(summary = "Get buildings by state", description = "Retrieve buildings in specific state")
    public ResponseEntity<BuildingListResponse> getBuildingsByState(@PathVariable("state") String state) {
        log.info("REST: Fetching buildings by state: {}", state);
        BuildingListResponse response = buildingService.getBuildingsByState(state);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update building", description = "Update building details (SYSTEM_ADMIN only)")
    public ResponseEntity<BuildingResponse> updateBuilding(
            @PathVariable("id") UUID buildingId,
            @Valid @RequestBody UpdateBuildingRequest request) {
        log.info("REST: Updating building: {}", buildingId);
        BuildingResponse response = buildingService.updateBuilding(buildingId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete building (soft delete)
     * DELETE /api/v1/buildings/{id}
     * Role: SYSTEM_ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Delete building", description = "Soft delete building (SYSTEM_ADMIN only)")
    public ResponseEntity<Void> deleteBuilding(@PathVariable("id") UUID buildingId) {
        log.info("REST: Deleting building: {}", buildingId);
        buildingService.deleteBuilding(buildingId);
        return ResponseEntity.noContent().build();
    }
}
