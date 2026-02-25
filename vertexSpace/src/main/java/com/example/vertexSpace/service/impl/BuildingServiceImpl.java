package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.request.CreateBuildingRequest;
import com.example.vertexSpace.dto.request.UpdateBuildingRequest;
import com.example.vertexSpace.dto.response.BuildingListResponse;
import com.example.vertexSpace.dto.response.BuildingResponse;
import com.example.vertexSpace.entity.Building;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.BuildingRepository;
import com.example.vertexSpace.service.BuildingService;
import com.example.vertexSpace.util.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of BuildingService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final ResourceMapper mapper;

    @Override
    public BuildingResponse createBuilding(CreateBuildingRequest request) {
        log.info("Creating building: {}", request.getName());

        // Check if building name already exists
        if (buildingRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Building with name '" + request.getName() + "' already exists");
        }

        Building building = new Building();
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setCity(request.getCity());
        building.setState(request.getState());
        building.setZipCode(request.getZipCode());
        building.setCountry(request.getCountry());
        building.setIsActive(true);

        Building saved = buildingRepository.save(building);
        log.info("Building created successfully: ID={}", saved.getId());

        return mapper.toBuildingResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponse getBuildingById(UUID buildingId) {
        log.info("Fetching building: {}", buildingId);

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + buildingId));

        return mapper.toBuildingResponse(building);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingListResponse getAllBuildings(boolean activeOnly) {
        log.info("Fetching all buildings (activeOnly={})", activeOnly);

        List<Building> buildings = activeOnly
                ? buildingRepository.findByIsActiveTrue()
                : buildingRepository.findAll();

        long activeCount = buildings.stream().filter(Building::getIsActive).count();

        return BuildingListResponse.builder()
                .buildings(mapper.toBuildingResponseList(buildings))
                .totalCount((long) buildings.size())
                .activeCount((int) activeCount)
                .build();
    }

    @Override
    public BuildingResponse updateBuilding(UUID buildingId, UpdateBuildingRequest request) {
        log.info("Updating building: {}", buildingId);

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + buildingId));

        // Check name uniqueness if name is being changed
        if (request.getName() != null && !request.getName().equalsIgnoreCase(building.getName())) {
            if (buildingRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), buildingId)) {
                throw new ValidationException("Building with name '" + request.getName() + "' already exists");
            }
            building.setName(request.getName());
        }

        // Update other fields if provided
        if (request.getAddress() != null) building.setAddress(request.getAddress());
        if (request.getCity() != null) building.setCity(request.getCity());
        if (request.getState() != null) building.setState(request.getState());
        if (request.getZipCode() != null) building.setZipCode(request.getZipCode());
        if (request.getCountry() != null) building.setCountry(request.getCountry());
        if (request.getIsActive() != null) building.setIsActive(request.getIsActive());

        Building updated = buildingRepository.save(building);
        log.info("Building updated successfully: ID={}", buildingId);

        return mapper.toBuildingResponse(updated);
    }

    @Override
    public void deleteBuilding(UUID buildingId) {
        log.info("Deleting building: {}", buildingId);

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + buildingId));

        // Check if building has any floors
        if (buildingRepository.hasFloors(buildingId)) {
            throw new ValidationException("Cannot delete building with existing floors. Delete floors first.");
        }

        // Soft delete
        building.setIsActive(false);
        buildingRepository.save(building);

        log.info("Building soft deleted: ID={}", buildingId);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingListResponse getBuildingsByCity(String city) {
        log.info("Fetching buildings by city: {}", city);

        List<Building> buildings = buildingRepository.findByCityIgnoreCaseAndIsActiveTrue(city);

        return BuildingListResponse.builder()
                .buildings(mapper.toBuildingResponseList(buildings))
                .totalCount((long) buildings.size())
                .activeCount(buildings.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingListResponse getBuildingsByState(String state) {
        log.info("Fetching buildings by state: {}", state);

        List<Building> buildings = buildingRepository.findByStateIgnoreCaseAndIsActiveTrue(state);

        return BuildingListResponse.builder()
                .buildings(mapper.toBuildingResponseList(buildings))
                .totalCount((long) buildings.size())
                .activeCount(buildings.size())
                .build();
    }
}
