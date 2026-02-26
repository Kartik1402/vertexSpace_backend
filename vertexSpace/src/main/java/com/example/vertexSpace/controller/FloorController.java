package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.request.CreateFloorRequest;
import com.example.vertexSpace.dto.request.UpdateFloorRequest;
import com.example.vertexSpace.dto.response.FloorListResponse;
import com.example.vertexSpace.dto.response.FloorResponse;
import com.example.vertexSpace.service.FloorService;
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
@RequestMapping("/api/v1/floors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Floors", description = "Floor management APIs (SYSTEM_ADMIN only)")
@SecurityRequirement(name = "bearer-auth")
public class FloorController {

    private final FloorService floorService;
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Create floor", description = "Create a new floor in a building (SYSTEM_ADMIN only)")
    public ResponseEntity<FloorResponse> createFloor(@Valid @RequestBody CreateFloorRequest request) {
        log.info("REST: Creating floor: {} in building {}", request.getFloorName(), request.getBuildingId());
        FloorResponse response = floorService.createFloor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get floor by ID", description = "Retrieve floor details by ID")
    public ResponseEntity<FloorResponse> getFloorById(@PathVariable("id") UUID floorId) {
        log.info("REST: Fetching floor: {}", floorId);
        FloorResponse response = floorService.getFloorById(floorId);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    @Operation(summary = "Get all floors", description = "Retrieve list of all floors")
    public ResponseEntity<FloorListResponse> getAllFloors(
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        log.info("REST: Fetching all floors (activeOnly={})", activeOnly);
        FloorListResponse response = floorService.getAllFloors(activeOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/building/{buildingId}")
    @Operation(summary = "Get floors by building", description = "Retrieve all floors in a specific building")
    public ResponseEntity<FloorListResponse> getFloorsByBuildingId(
            @PathVariable("buildingId") UUID buildingId,
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        log.info("REST: Fetching floors for building: {} (activeOnly={})", buildingId, activeOnly);
        FloorListResponse response = floorService.getFloorsByBuildingId(buildingId, activeOnly);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Update floor", description = "Update floor details (SYSTEM_ADMIN only)")
    public ResponseEntity<FloorResponse> updateFloor(
            @PathVariable("id") UUID floorId,
            @Valid @RequestBody UpdateFloorRequest request) {
        log.info("REST: Updating floor: {}", floorId);
        FloorResponse response = floorService.updateFloor(floorId, request);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Delete floor", description = "Soft delete floor (SYSTEM_ADMIN only)")
    public ResponseEntity<Void> deleteFloor(@PathVariable("id") UUID floorId) {
        log.info("REST: Deleting floor: {}", floorId);
        floorService.deleteFloor(floorId);
        return ResponseEntity.noContent().build();
    }
}
