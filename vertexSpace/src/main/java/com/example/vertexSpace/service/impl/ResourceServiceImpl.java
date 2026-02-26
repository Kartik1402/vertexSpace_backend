package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.request.CreateResourceRequest;
import com.example.vertexSpace.dto.request.ResourceFilterRequest;
import com.example.vertexSpace.dto.request.UpdateResourceRequest;
import com.example.vertexSpace.dto.response.ResourceAvailabilityResponse;
import com.example.vertexSpace.dto.response.ResourceListResponse;
import com.example.vertexSpace.dto.response.ResourceResponse;
import com.example.vertexSpace.dto.response.ResourceStatisticsResponse;
import com.example.vertexSpace.repository.DeskAssignmentRepository;
import com.example.vertexSpace.entity.Department;
import com.example.vertexSpace.entity.Floor;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.ResourceTimeBlock;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import com.example.vertexSpace.enums.Role;
import com.example.vertexSpace.exception.AuthorizationException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.repository.DepartmentRepository;
import com.example.vertexSpace.repository.FloorRepository;
import com.example.vertexSpace.repository.ResourceRepository;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.service.ResourceService;
import com.example.vertexSpace.util.BookingValidator;
import com.example.vertexSpace.util.ResourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final FloorRepository floorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ResourceTimeBlockRepository timeBlockRepository;
    private final ResourceMapper mapper;
    private final BookingValidator bookingValidator;
    private final DeskAssignmentRepository deskAssignmentRepository;

    @Override
    public ResourceResponse createResource(CreateResourceRequest request, UUID currentUserId) {
        log.info("Creating resource: {} (Type: {}) by user: {}",
                request.getName(), request.getResourceType(), currentUserId);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        // Validate floor exists
        Floor floor = floorRepository.findByIdWithBuilding(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with ID: " + request.getFloorId()));

        if (!floor.getIsActive() || !floor.getBuilding().getIsActive()) {
            throw new ValidationException("Cannot create resource on inactive floor or building");
        }

        Department department = departmentRepository.findById(request.getOwningDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getOwningDepartmentId()));

        if (!department.getIsActive()) {
            throw new ValidationException("Cannot assign resource to inactive department");
        }

        // Authorization: DEPT_ADMIN can only create in own department
        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (!currentUser.getDepartment().getId().equals(request.getOwningDepartmentId())) {
                throw new AuthorizationException("Department admins can only create resources in their own department");
            }
        }

        // Check if resource name already exists on this floor
        if (resourceRepository.existsByFloorIdAndNameIgnoreCase(request.getFloorId(), request.getName())) {
            throw new ValidationException(
                    String.format("Resource with name '%s' already exists on floor '%s'",
                            request.getName(), floor.getFloorName())
            );
        }

        if (request.getResourceType() == ResourceType.ROOM) {
            if (request.getCapacity() == null || request.getCapacity() <= 0) {
                throw new ValidationException("Capacity is required and must be positive for ROOM type");
            }
        }
        AssignmentMode assignmentMode = determineAssignmentMode(
                request.getResourceType(),
                request.getAssignmentMode()
        );

        // ✅ Validate desk-specific rules
        if (request.getResourceType() == ResourceType.DESK) {
            validateDeskMode(assignmentMode);
        }


        Resource resource = new Resource();
        resource.setFloor(floor);
        resource.setOwningDepartment(department);
        resource.setResourceType(request.getResourceType());
        resource.setAssignmentMode(assignmentMode); // ✅ Set assignment mode
        resource.setName(request.getName());
        resource.setCapacity(request.getResourceType() == ResourceType.ROOM ? request.getCapacity() : null);
        resource.setDescription(request.getDescription());
        resource.setIsActive(true);

        Resource saved = resourceRepository.save(resource);
        log.info("Resource created successfully: ID={}, Type={}", saved.getId(), saved.getResourceType());

        return mapper.toResourceResponse(saved);
    }
    private AssignmentMode determineAssignmentMode(ResourceType type, AssignmentMode requested) {
        // For non-desk resources, always use NOT_APPLICABLE
        if (type != ResourceType.DESK) {
            return AssignmentMode.NOT_APPLICABLE;
        }

        return requested != null ? requested : AssignmentMode.HOT_DESK;
    }

    private void validateDeskMode(AssignmentMode mode) {
        if (mode == AssignmentMode.NOT_APPLICABLE) {
            throw new ValidationException("Desks must be either ASSIGNED or HOT_DESK");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(UUID resourceId) {
        log.info("Fetching resource: {}", resourceId);

        Resource resource = resourceRepository.findByIdWithRelations(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        return mapper.toResourceResponse(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceListResponse getResources(ResourceFilterRequest filterRequest) {
        log.info("Fetching resources with filters: {}", filterRequest);

        List<Resource> resources;

        if (filterRequest == null || !filterRequest.hasFilters()) {
            // No filters - return all active resources
            resources = resourceRepository.findByIsActiveTrue();
        } else if (filterRequest.getAvailable() != null && filterRequest.getAvailable()) {
            // ⚠️ NOTE: This uses the OLD method signature (findAvailableByFilters)
            // modify this or create a separate endpoint
            resources = resourceRepository.findAvailableByFilters(
                    filterRequest.getType(),
                    filterRequest.getBuildingId(),
                    filterRequest.getFloorId(),
                    filterRequest.getDepartmentId(),
                    filterRequest.getMinCapacity(),
                    filterRequest.getStartTime(),
                    filterRequest.getEndTime()
            );
        } else {
            String typeString = filterRequest.getType() != null
                    ? filterRequest.getType().name()
                    : null;

            resources = resourceRepository.findByFilters(
                    filterRequest.getType(),
                    filterRequest.getFloorId(),
                    filterRequest.getBuildingId(),
                    filterRequest.getDepartmentId(),
                    filterRequest.getMinCapacity()
            );
        }

        // Count by type
        Map<String, Long> countByType = resources.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getResourceType().name(),
                        Collectors.counting()
                ));

        long activeCount = resources.stream().filter(Resource::getIsActive).count();

        return ResourceListResponse.builder()
                .resources(mapper.toResourceResponseList(resources))
                .totalCount((long) resources.size())
                .activeCount((int) activeCount)
                .countByType(countByType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceListResponse getResourcesByType(ResourceType type) {
        log.info("Fetching resources by type: {}", type);

        List<Resource> resources = resourceRepository.findByResourceTypeAndIsActiveTrue(type);

        return ResourceListResponse.builder()
                .resources(mapper.toResourceResponseList(resources))
                .totalCount((long) resources.size())
                .activeCount(resources.size())
                .countByType(Map.of(type.name(), (long) resources.size()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceListResponse getResourcesByDepartment(UUID departmentId) {
        log.info("Fetching resources for department: {}", departmentId);

        // Validate department exists
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        List<Resource> resources = resourceRepository.findByOwningDepartmentIdAndIsActiveTrue(departmentId);

        Map<String, Long> countByType = resources.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getResourceType().name(),
                        Collectors.counting()
                ));

        return ResourceListResponse.builder()
                .resources(mapper.toResourceResponseList(resources))
                .totalCount((long) resources.size())
                .activeCount(resources.size())
                .countByType(countByType)
                .build();
    }

    @Override
    public ResourceResponse updateResource(UUID resourceId, UpdateResourceRequest request, UUID currentUserId) {
        log.info("Updating resource: {} by user: {}", resourceId, currentUserId);

        // Get current user for authorization
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        Resource resource = resourceRepository.findByIdWithRelations(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        // Authorization: DEPT_ADMIN can only update resources in own department
        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (!resource.getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new AuthorizationException("Department admins can only update resources in their own department");
            }
        }

        // Update floor if provided
        if (request.getFloorId() != null && !request.getFloorId().equals(resource.getFloor().getId())) {
            Floor newFloor = floorRepository.findByIdWithBuilding(request.getFloorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Floor not found with ID: " + request.getFloorId()));

            if (!newFloor.getIsActive() || !newFloor.getBuilding().getIsActive()) {
                throw new ValidationException("Cannot move resource to inactive floor or building");
            }

            resource.setFloor(newFloor);
        }

        // Update department if provided
        if (request.getOwningDepartmentId() != null &&
                !request.getOwningDepartmentId().equals(resource.getOwningDepartment().getId())) {

            // DEPT_ADMIN cannot change department
            if (currentUser.getRole() == Role.DEPT_ADMIN) {
                throw new AuthorizationException("Department admins cannot transfer resources to other departments");
            }

            Department newDepartment = departmentRepository.findById(request.getOwningDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getOwningDepartmentId()));

            if (!newDepartment.getIsActive()) {
                throw new ValidationException("Cannot transfer resource to inactive department");
            }

            resource.setOwningDepartment(newDepartment);
        }

        // Update resource type if provided
        if (request.getResourceType() != null && request.getResourceType() != resource.getResourceType()) {
            resource.setResourceType(request.getResourceType());

            // If changing to ROOM, capacity is required
            if (request.getResourceType() == ResourceType.ROOM && request.getCapacity() == null) {
                throw new ValidationException("Capacity is required when changing to ROOM type");
            }
        }

        // Update name if provided
        if (request.getName() != null && !request.getName().equalsIgnoreCase(resource.getName())) {
            if (resourceRepository.existsByFloorIdAndNameIgnoreCaseAndIdNot(
                    resource.getFloor().getId(), request.getName(), resourceId)) {
                throw new ValidationException(
                        String.format("Resource with name '%s' already exists on floor '%s'",
                                request.getName(), resource.getFloor().getFloorName())
                );
            }
            resource.setName(request.getName());
        }

        // Update capacity if provided
        if (request.getCapacity() != null) {
            if (resource.getResourceType() == ResourceType.ROOM || request.getResourceType() == ResourceType.ROOM) {
                resource.setCapacity(request.getCapacity());
            }
        }

        // Update other fields
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getIsActive() != null) resource.setIsActive(request.getIsActive());

        Resource updated = resourceRepository.save(resource);
        log.info("Resource updated successfully: ID={}", resourceId);

        return mapper.toResourceResponse(updated);
    }

    @Override
    public void deleteResource(UUID resourceId, UUID currentUserId) {
        log.info("Deleting resource: {} by user: {}", resourceId, currentUserId);

        // Get current user for authorization
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        // Authorization: DEPT_ADMIN can only delete resources in own department
        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (!resource.getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new AuthorizationException("Department admins can only delete resources in their own department");
            }
        }

        // Check if resource has active bookings
        long activeBookings = timeBlockRepository.countActiveBookings(resourceId);
        if (activeBookings > 0) {
            throw new ValidationException(
                    String.format("Cannot delete resource with %d active booking(s). Cancel bookings first.", activeBookings)
            );
        }

        // Soft delete
        resource.setIsActive(false);
        resourceRepository.save(resource);

        log.info("Resource soft deleted: ID={}", resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAvailabilityResponse checkAvailability(UUID resourceId, Instant startTime, Instant endTime) {
        log.info("Checking availability for resource: {} from {} to {}", resourceId, startTime, endTime);

        // Validate resource exists
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        // Validate time range
        bookingValidator.validateBookingTime(startTime, endTime);

        // Calculate conflict end time (using default buffer of 15 minutes)
        Instant conflictEndTime = bookingValidator.calculateConflictEndTime(endTime, 15);

        // Find overlapping blocks
        List<ResourceTimeBlock> conflicts = timeBlockRepository.findOverlappingBlocks(
                resourceId, startTime, conflictEndTime
        );

        boolean isAvailable = conflicts.isEmpty();

        return ResourceAvailabilityResponse.builder()
                .resourceId(resourceId)
                .resourceName(resource.getName())
                .isAvailable(isAvailable)
                .requestedStartTime(startTime)
                .requestedEndTime(endTime)
                .conflicts(conflicts.isEmpty() ? null :
                        conflicts.stream()
                                .map(mapper::toConflictingBooking)
                                .collect(Collectors.toList())
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceStatisticsResponse getResourceStatistics(UUID resourceId) {
        log.info("Fetching statistics for resource: {}", resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        Instant now = Instant.now();

        // Get all bookings
        List<ResourceTimeBlock> allBookings = timeBlockRepository.findResourceBookings(resourceId);

        // Split into upcoming and past
        long upcomingCount = allBookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        long pastCount = allBookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        // Calculate utilization (simplified - can be enhanced)
        double utilizationRate = calculateUtilizationRate(allBookings);

        return ResourceStatisticsResponse.builder()
                .resourceId(resourceId)
                .resourceName(resource.getName())
                .totalBookings((long) allBookings.size())
                .upcomingBookings(upcomingCount)
                .pastBookings(pastCount)
                .utilizationRate(utilizationRate)
                .build();
    }

    private double calculateUtilizationRate(List<ResourceTimeBlock> bookings) {
        // Simplified calculation - percentage of days with bookings in last 30 days
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minusSeconds(30 * 24 * 60 * 60);

        long recentBookings = bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(thirtyDaysAgo))
                .count();

        // Very simplified: assume max 10 bookings per day for 30 days = 300 max
        return Math.min(100.0, (recentBookings / 300.0) * 100.0);
    }
    @Transactional
    public ResourceResponse changeAssignmentMode(UUID resourceId, AssignmentMode newMode, UUID currentUserId) {
        log.info("Changing assignment mode for resource {} to {} by user {}",
                resourceId, newMode, currentUserId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Only System Admin can change modes
        if (currentUser.getRole() != Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("Only System Admin can change assignment modes");
        }

        // ✅ Only desks can have assignment modes changed
        if (resource.getResourceType() != ResourceType.DESK) {
            throw new ValidationException("Only desks can have assignment modes. This is a " + resource.getResourceType());
        }

        // ✅ Validate new mode
        if (newMode == AssignmentMode.NOT_APPLICABLE) {
            throw new ValidationException("Desks must be either ASSIGNED or HOT_DESK");
        }

        // ✅ Check for conflicts before changing
        if (newMode == AssignmentMode.ASSIGNED) {
            // Warn if there are active bookings
            long activeBookings = timeBlockRepository.countActiveBookings(resourceId);
            if (activeBookings > 0) {
                log.warn("Resource {} has {} active bookings. Changing to ASSIGNED mode.",
                        resourceId, activeBookings);
                // You might want to throw an exception here or require force flag
            }
        }

        if (newMode == AssignmentMode.HOT_DESK) {
            // Warn if there are active assignments
            long activeAssignments = deskAssignmentRepository.countActiveAssignmentsForDesk(resourceId, Instant.now());
            if (activeAssignments > 0) {
                log.warn("Desk {} has {} active assignments. Changing to HOT_DESK mode.",
                        resourceId, activeAssignments);
                // You might want to throw an exception here or require force flag
            }
        }

        resource.setAssignmentMode(newMode);
        resource = resourceRepository.save(resource);

        log.info("Assignment mode changed successfully for resource {}", resourceId);

        return mapper.toResourceResponse(resource);
    }

}
