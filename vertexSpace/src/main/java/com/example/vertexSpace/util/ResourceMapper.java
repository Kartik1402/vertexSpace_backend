package com.example.vertexSpace.util;

import com.example.vertexSpace.dto.response.*;
import com.example.vertexSpace.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping entities to DTOs
 */
@Component
public class ResourceMapper {

    // ========================================================================
    // BUILDING MAPPERS
    // ========================================================================

    public BuildingResponse toBuildingResponse(Building building) {
        return BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .address(building.getAddress())
                .city(building.getCity())
                .state(building.getState())
                .zipCode(building.getZipCode())
                .country(building.getCountry())
                .isActive(building.getIsActive())
                .createdAt(building.getCreatedAtUtc())
                .updatedAt(building.getUpdatedAtUtc())
                .build();
    }

    public List<BuildingResponse> toBuildingResponseList(List<Building> buildings) {
        return buildings.stream()
                .map(this::toBuildingResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // FLOOR MAPPERS
    // ========================================================================

    public FloorResponse toFloorResponse(Floor floor) {
        return FloorResponse.builder()
                .id(floor.getId())
                .buildingId(floor.getBuilding().getId())
                .buildingName(floor.getBuilding().getName())
                .floorNumber(floor.getFloorNumber())
                .floorName(floor.getFloorName())
                .isActive(floor.getIsActive())
                .createdAt(floor.getCreatedAtUtc())
                .updatedAt(floor.getUpdatedAtUtc())
                .build();
    }

    public List<FloorResponse> toFloorResponseList(List<Floor> floors) {
        return floors.stream()
                .map(this::toFloorResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // RESOURCE MAPPERS
    // ========================================================================

    public ResourceResponse toResourceResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                // Floor details
                .floorId(resource.getFloor().getId())
                .floorName(resource.getFloor().getFloorName())
                .floorNumber(resource.getFloor().getFloorNumber())
                // Building details
                .buildingId(resource.getFloor().getBuilding().getId())
                .buildingName(resource.getFloor().getBuilding().getName())
                // Department details
                .owningDepartmentId(resource.getOwningDepartment().getId())
                .owningDepartmentName(resource.getOwningDepartment().getName())
                .owningDepartmentCode(resource.getOwningDepartment().getCode())
                // Resource details
                .resourceType(resource.getResourceType())
                .name(resource.getName())
                .capacity(resource.getCapacity())
                .description(resource.getDescription())
                .isActive(resource.getIsActive())
                // Audit fields
                .createdAt(resource.getCreatedAtUtc())
                .updatedAt(resource.getUpdatedAtUtc())
                .build();
    }

    public List<ResourceResponse> toResourceResponseList(List<Resource> resources) {
        return resources.stream()
                .map(this::toResourceResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // BOOKING MAPPERS
    // ========================================================================

    public BookingResponse toBookingResponse(ResourceTimeBlock block) {
        return BookingResponse.builder()
                .id(block.getId())
                // Resource details
                .resourceId(block.getResource().getId())
                .resourceName(block.getResource().getName())
                .resourceType(block.getResource().getResourceType())
                // Location details
                .buildingName(block.getResource().getFloor().getBuilding().getName())
                .floorName(block.getResource().getFloor().getFloorName())
                // User details
                .userId(block.getUser().getId())
                .userDisplayName(block.getUser().getDisplayName())
                .userEmail(block.getUser().getEmail())
                .userDepartmentName(block.getUser().getDepartment().getName())
                // Booking details
                .blockType(block.getBlockType())
                .status(block.getStatus())
                .startTime(block.getStartTimeUtc())
                .endTime(block.getEndTimeUtc())
                .conflictEndTime(block.getConflictEndUtc())
                .bufferMinutes(block.getBufferMinutes())
                .purpose(block.getPurpose())
                // Audit fields
                .createdAt(block.getCreatedAtUtc())
                .updatedAt(block.getUpdatedAtUtc())
                .build();
    }

    public List<BookingResponse> toBookingResponseList(List<ResourceTimeBlock> blocks) {
        return blocks.stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // AVAILABILITY MAPPER
    // ========================================================================

    public ResourceAvailabilityResponse.ConflictingBooking toConflictingBooking(ResourceTimeBlock block) {
        return ResourceAvailabilityResponse.ConflictingBooking.builder()
                .bookingId(block.getId())
                .startTime(block.getStartTimeUtc())
                .endTime(block.getEndTimeUtc())
                .conflictEndTime(block.getConflictEndUtc())
                .bookedByUser(block.getUser().getDisplayName())
                .build();
    }
}
