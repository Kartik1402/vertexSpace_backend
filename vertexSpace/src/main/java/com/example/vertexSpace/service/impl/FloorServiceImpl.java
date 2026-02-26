package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.request.CreateFloorRequest;
import com.example.vertexSpace.dto.request.UpdateFloorRequest;
import com.example.vertexSpace.dto.response.FloorListResponse;
import com.example.vertexSpace.dto.response.FloorResponse;
import com.example.vertexSpace.entity.Building;
import com.example.vertexSpace.entity.Floor;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.BuildingRepository;
import com.example.vertexSpace.repository.FloorRepository;
import com.example.vertexSpace.service.FloorService;
import com.example.vertexSpace.util.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final ResourceMapper mapper;

    @Override
    public FloorResponse createFloor(CreateFloorRequest request) {
        log.info("Creating floor: {} in building {}", request.getFloorName(), request.getBuildingId());

        // Validate building exists
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + request.getBuildingId()));

        if (!building.getIsActive()) {
            throw new ValidationException("Cannot create floor in inactive building");
        }

        // Check if floor number already exists in building
        if (floorRepository.existsByBuildingIdAndFloorNumber(request.getBuildingId(), request.getFloorNumber())) {
            throw new ValidationException(
                    String.format("Floor number %d already exists in building '%s'",
                            request.getFloorNumber(), building.getName())
            );
        }

        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setFloorNumber(request.getFloorNumber());
        floor.setFloorName(request.getFloorName());
        floor.setIsActive(true);

        Floor saved = floorRepository.save(floor);
        log.info("Floor created successfully: ID={}", saved.getId());

        return mapper.toFloorResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FloorResponse getFloorById(UUID floorId) {
        log.info("Fetching floor: {}", floorId);

        Floor floor = floorRepository.findByIdWithBuilding(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with ID: " + floorId));

        return mapper.toFloorResponse(floor);
    }

    @Override
    @Transactional(readOnly = true)
    public FloorListResponse getAllFloors(boolean activeOnly) {
        log.info("Fetching all floors (activeOnly={})", activeOnly);

        List<Floor> floors = activeOnly
                ? floorRepository.findByIsActiveTrue()
                : floorRepository.findAll();

        long activeCount = floors.stream().filter(Floor::getIsActive).count();

        return FloorListResponse.builder()
                .floors(mapper.toFloorResponseList(floors))
                .totalCount((long) floors.size())
                .activeCount((int) activeCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FloorListResponse getFloorsByBuildingId(UUID buildingId, boolean activeOnly) {
        log.info("Fetching floors for building: {} (activeOnly={})", buildingId, activeOnly);

        // Validate building exists
        buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + buildingId));

        List<Floor> floors = activeOnly
                ? floorRepository.findByBuildingIdAndIsActiveTrue(buildingId)
                : floorRepository.findByBuildingId(buildingId);

        long activeCount = floors.stream().filter(Floor::getIsActive).count();

        return FloorListResponse.builder()
                .floors(mapper.toFloorResponseList(floors))
                .totalCount((long) floors.size())
                .activeCount((int) activeCount)
                .build();
    }

    @Override
    public FloorResponse updateFloor(UUID floorId, UpdateFloorRequest request) {
        log.info("Updating floor: {}", floorId);

        Floor floor = floorRepository.findByIdWithBuilding(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with ID: " + floorId));

        // Update building if provided
        if (request.getBuildingId() != null && !request.getBuildingId().equals(floor.getBuilding().getId())) {
            Building newBuilding = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + request.getBuildingId()));

            if (!newBuilding.getIsActive()) {
                throw new ValidationException("Cannot move floor to inactive building");
            }

            floor.setBuilding(newBuilding);
        }

        // Update floor number if provided
        if (request.getFloorNumber() != null && !request.getFloorNumber().equals(floor.getFloorNumber())) {
            if (floorRepository.existsByBuildingIdAndFloorNumberAndIdNot(
                    floor.getBuilding().getId(), request.getFloorNumber(), floorId)) {
                throw new ValidationException(
                        String.format("Floor number %d already exists in building '%s'",
                                request.getFloorNumber(), floor.getBuilding().getName())
                );
            }
            floor.setFloorNumber(request.getFloorNumber());
        }

        // Update other fields if provided
        if (request.getFloorName() != null) floor.setFloorName(request.getFloorName());
        if (request.getIsActive() != null) floor.setIsActive(request.getIsActive());

        Floor updated = floorRepository.save(floor);
        log.info("Floor updated successfully: ID={}", floorId);

        return mapper.toFloorResponse(updated);
    }

    @Override
    public void deleteFloor(UUID floorId) {
        log.info("Deleting floor: {}", floorId);

        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with ID: " + floorId));

        // Check if floor has any resources
        if (floorRepository.hasResources(floorId)) {
            throw new ValidationException("Cannot delete floor with existing resources. Delete resources first.");
        }

        // Soft delete
        floor.setIsActive(false);
        floorRepository.save(floor);

        log.info("Floor soft deleted: ID={}", floorId);
    }
}
