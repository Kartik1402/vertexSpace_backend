package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.deskassignment.DeskAssignmentRequestDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentResponseDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentSummaryDTO;
import com.example.vertexSpace.dto.deskassignment.UpdateDeskAssignmentRequestDTO;
import com.example.vertexSpace.entity.DeskAssignment;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import com.example.vertexSpace.enums.Role;
import com.example.vertexSpace.exception.AuthorizationException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.repository.DeskAssignmentRepository;
import com.example.vertexSpace.repository.ResourceRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.service.DeskAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeskAssignmentServiceImpl implements DeskAssignmentService {

    private final DeskAssignmentRepository assignmentRepo;
    private final ResourceRepository resourceRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public DeskAssignmentResponseDTO createAssignment(DeskAssignmentRequestDTO request, UUID currentUserId) {
        log.info("Creating desk assignment: desk={}, user={}, start={}",
                request.getDeskId(), request.getUserId(), request.getStartUtc());

        // Fetch entities
        Resource desk = resourceRepo.findById(request.getDeskId())
                .orElseThrow(() -> new ResourceNotFoundException("Desk not found: " + request.getDeskId()));

        User assignedUser = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        // ✅ Validation 1: Must be a desk
        if (desk.getResourceType() != ResourceType.DESK) {
            throw new ValidationException("Resource is not a desk. It's a " + desk.getResourceType());
        }

        // ✅ Validation 2: Desk must be in ASSIGNED mode
        if (desk.getAssignmentMode() != AssignmentMode.ASSIGNED) {
            throw new ValidationException(
                    "This desk is in " + desk.getAssignmentMode() + " mode. " +
                            "Only ASSIGNED desks can have permanent assignments. " +
                            "Please use booking system for HOT_DESK resources."
            );
        }

        // ✅ Validation 3: Authorization check
        validateAssignmentPermission(currentUser, desk);

        // ✅ Validation 4: Date validation
        validateAssignmentDates(request.getStartUtc(), request.getEndUtc());

        // ✅ Validation 5: Check for overlaps
        Instant effectiveEndTime = request.getEndUtc() != null ? request.getEndUtc() : Instant.MAX;
        long overlaps = assignmentRepo.countOverlappingAssignments(
                request.getDeskId(),
                request.getStartUtc(),
                effectiveEndTime,
                UUID.randomUUID() // No existing assignment to exclude
        );

        if (overlaps > 0) {
            throw new ValidationException(
                    "Desk already has an assignment during this time period. " +
                            "Assignments cannot overlap."
            );
        }

        // Create assignment
        DeskAssignment assignment = DeskAssignment.builder()
                .resource(desk)
                .user(assignedUser)
                .startUtc(request.getStartUtc())
                .endUtc(request.getEndUtc())
                .notes(request.getNotes())
                .assignedBy(currentUser)
                .isActive(true)
                .build();

        assignment = assignmentRepo.save(assignment);

        log.info("Desk assignment created: {} for user {} on desk {}",
                assignment.getId(), assignedUser.getEmail(), desk.getName());

        return toDTO(assignment);
    }

    @Override
    @Transactional
    public DeskAssignmentResponseDTO updateAssignment(
            UUID assignmentId,
            UpdateDeskAssignmentRequestDTO request,
            UUID currentUserId) {

        log.info("Updating desk assignment: {}", assignmentId);

        DeskAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Authorization check
        validateAssignmentPermission(currentUser, assignment.getResource());

        // ✅ Validate new end date if provided
        if (request.getEndUtc() != null) {
            if (request.getEndUtc().isBefore(assignment.getStartUtc())) {
                throw new ValidationException("End date cannot be before start date");
            }

            // Check for overlaps with new end date
            Instant effectiveEndTime = request.getEndUtc();
            long overlaps = assignmentRepo.countOverlappingAssignments(
                    assignment.getResource().getId(),
                    assignment.getStartUtc(),
                    effectiveEndTime,
                    assignmentId // Exclude current assignment
            );

            if (overlaps > 0) {
                throw new ValidationException(
                        "Cannot update: would create overlap with another assignment"
                );
            }

            assignment.setEndUtc(request.getEndUtc());
        }

        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }

        assignment = assignmentRepo.save(assignment);

        log.info("Assignment updated: {}", assignmentId);

        return toDTO(assignment);
    }

    @Override
    @Transactional
    public void deleteAssignment(UUID assignmentId, UUID currentUserId) {
        log.info("Deleting desk assignment: {}", assignmentId);

        DeskAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Authorization check
        validateAssignmentPermission(currentUser, assignment.getResource());

        // Soft delete
        int updated = assignmentRepo.deactivateAssignment(assignmentId);

        if (updated == 0) {
            throw new ValidationException("Failed to delete assignment");
        }

        log.info("Assignment deleted (deactivated): {}", assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DeskAssignmentResponseDTO getAssignmentById(UUID assignmentId, UUID currentUserId) {
        DeskAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Check if user can view this assignment
        validateViewPermission(currentUser, assignment);

        return toDTO(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeskAssignmentResponseDTO> getAssignmentsByDesk(
            UUID deskId,
            UUID currentUserId,
            Pageable pageable) {

        Resource desk = resourceRepo.findById(deskId)
                .orElseThrow(() -> new ResourceNotFoundException("Desk not found: " + deskId));

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Check permission to view assignments for this desk
        if (currentUser.getRole() == Role.USER) {
            // Regular users can only see their own assignments
            return (Page<DeskAssignmentResponseDTO>) assignmentRepo.findByDeskId(deskId, pageable)
                    .map(this::toDTO)
                    .filter(dto -> dto.getUserId().equals(currentUserId));
        }

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            // Department Admin can only see their department's desks
            if (!desk.getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new AuthorizationException("You can only view assignments in your department");
            }
        }

        return assignmentRepo.findByDeskId(deskId, pageable)
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeskAssignmentResponseDTO> getAssignmentsByDepartment(
            UUID departmentId,
            UUID currentUserId,
            Pageable pageable) {

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Authorization: Only Department Admin (of that dept) or System Admin
        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (!currentUser.getDepartment().getId().equals(departmentId)) {
                throw new AuthorizationException("You can only view assignments in your own department");
            }
        } else if (currentUser.getRole() == Role.USER) {
            throw new AuthorizationException("Only admins can view department assignments");
        }

        return assignmentRepo.findByDepartmentId(departmentId, pageable)
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DeskAssignmentSummaryDTO getMyCurrentAssignment(UUID userId) {
        Optional<DeskAssignment> assignment = assignmentRepo.findCurrentAssignmentForUser(userId, Instant.now());

        if (assignment.isEmpty()) {
            return null; // No current assignment
        }

        DeskAssignment da = assignment.get();
        return DeskAssignmentSummaryDTO.builder()
                .assignmentId(da.getId())
                .deskName(da.getResource().getName())
                .departmentName(da.getResource().getOwningDepartment().getName())
                .isIndefinite(da.isIndefinite())
                .isCurrentlyActive(da.isCurrentlyActive())
                .startUtc(da.getStartUtc())
                .endUtc(da.getEndUtc())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeskAssignmentResponseDTO> getMyAssignments(UUID userId) {
        List<DeskAssignment> assignments = assignmentRepo.findActiveAssignmentsByUserId(userId);
        return assignments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeskAvailableForAssignment(UUID deskId, Instant startUtc, Instant endUtc) {
        Instant effectiveEndTime = endUtc != null ? endUtc : Instant.MAX;
        long overlaps = assignmentRepo.countOverlappingAssignments(
                deskId,
                startUtc,
                effectiveEndTime,
                UUID.randomUUID() // No existing assignment to exclude
        );
        return overlaps == 0;
    }

    // ===================================================================
    // Private Helper Methods
    // ===================================================================

    private void validateAssignmentPermission(User currentUser, Resource desk) {
        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return; // System Admin can do anything
        }

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            // Department Admin can only manage desks in their department
            if (!desk.getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                throw new AuthorizationException(
                        "You can only manage desk assignments in your own department"
                );
            }
            return;
        }

        throw new AuthorizationException("Only admins can manage desk assignments");
    }

    private void validateViewPermission(User currentUser, DeskAssignment assignment) {
        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return; // System Admin can view anything
        }

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            // Department Admin can view their department's assignments
            if (assignment.getResource().getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                return;
            }
        }

        // Regular users can only view their own assignments
        if (assignment.getUser().getId().equals(currentUser.getId())) {
            return;
        }

        throw new AuthorizationException("You don't have permission to view this assignment");
    }

    private void validateAssignmentDates(Instant startUtc, Instant endUtc) {
        Instant now = Instant.now();

        if (startUtc.isBefore(now.minusSeconds(300))) { // Allow 5 min buffer
            throw new ValidationException("Start date cannot be in the past");
        }

        if (endUtc != null && endUtc.isBefore(startUtc)) {
            throw new ValidationException("End date must be after start date");
        }
    }

    private DeskAssignmentResponseDTO toDTO(DeskAssignment assignment) {
        return DeskAssignmentResponseDTO.builder()
                .id(assignment.getId())
                .deskId(assignment.getResource().getId())
                .deskName(assignment.getResource().getName())
                .departmentId(assignment.getResource().getOwningDepartment().getId())
                .departmentName(assignment.getResource().getOwningDepartment().getName())
                .userId(assignment.getUser().getId())
                .userEmail(assignment.getUser().getEmail())
                .userDisplayName(assignment.getUser().getDisplayName())
                .startUtc(assignment.getStartUtc())
                .endUtc(assignment.getEndUtc())
                .isIndefinite(assignment.isIndefinite())
                .isCurrentlyActive(assignment.isCurrentlyActive())
                .isActive(assignment.getIsActive())
                .notes(assignment.getNotes())
                .assignedByUserId(assignment.getAssignedBy().getId())
                .assignedByEmail(assignment.getAssignedBy().getEmail())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
