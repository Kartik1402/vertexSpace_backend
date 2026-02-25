package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.deskassignment.DeskAssignmentRequestDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentResponseDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentSummaryDTO;
import com.example.vertexSpace.dto.deskassignment.UpdateDeskAssignmentRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeskAssignmentService {

    /**
     * Create a new desk assignment
     * System Admin: can assign any desk
     * Department Admin: can assign desks in their department only
     */
    DeskAssignmentResponseDTO createAssignment(DeskAssignmentRequestDTO request, UUID currentUserId);

    /**
     * Update an existing assignment (change end date or notes)
     */
    DeskAssignmentResponseDTO updateAssignment(UUID assignmentId, UpdateDeskAssignmentRequestDTO request, UUID currentUserId);

    /**
     * Delete (deactivate) an assignment
     */
    void deleteAssignment(UUID assignmentId, UUID currentUserId);

    /**
     * Get assignment details
     */
    DeskAssignmentResponseDTO getAssignmentById(UUID assignmentId, UUID currentUserId);

    /**
     * Get all assignments for a specific desk
     */
    Page<DeskAssignmentResponseDTO> getAssignmentsByDesk(UUID deskId, UUID currentUserId, Pageable pageable);

    /**
     * Get all assignments in a department (Department Admin and System Admin only)
     */
    Page<DeskAssignmentResponseDTO> getAssignmentsByDepartment(UUID departmentId, UUID currentUserId, Pageable pageable);

    /**
     * Get my current desk assignment
     */
    DeskAssignmentSummaryDTO getMyCurrentAssignment(UUID userId);

    /**
     * Get all my desk assignments (past and present)
     */
    List<DeskAssignmentResponseDTO> getMyAssignments(UUID userId);

    /**
     * Check if a desk is available for assignment in a time range
     */
    boolean isDeskAvailableForAssignment(UUID deskId, java.time.Instant startUtc, java.time.Instant endUtc);
}
