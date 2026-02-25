package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.deskassignment.DeskAssignmentRequestDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentResponseDTO;
import com.example.vertexSpace.dto.deskassignment.DeskAssignmentSummaryDTO;
import com.example.vertexSpace.dto.deskassignment.UpdateDeskAssignmentRequestDTO;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.DeskAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/desk-assignments")
@RequiredArgsConstructor
@Tag(name = "Desk Assignments", description = "Manage permanent desk assignments (ASSIGNED desks only)")
@SecurityRequirement(name = "bearer-auth")
public class DeskAssignmentController {

    private final DeskAssignmentService assignmentService;
    private final AuthService authService;

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return authService.getUserIdByEmail(authentication.getName());
    }

    @PostMapping
    @Operation(
            summary = "Create desk assignment",
            description = "Assign a desk to a user. " +
                    "System Admin: can assign any desk. " +
                    "Department Admin: can assign desks in their department only. " +
                    "Only works for desks in ASSIGNED mode."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assignment created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or desk already assigned"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Desk or user not found")
    })
    public ResponseEntity<DeskAssignmentResponseDTO> createAssignment(
            @Valid @RequestBody DeskAssignmentRequestDTO request,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        DeskAssignmentResponseDTO response = assignmentService.createAssignment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{assignmentId}")
    @Operation(
            summary = "Update desk assignment",
            description = "Update end date or notes for an existing assignment"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment updated"),
            @ApiResponse(responseCode = "400", description = "Invalid update request"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<DeskAssignmentResponseDTO> updateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateDeskAssignmentRequestDTO request,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        DeskAssignmentResponseDTO response = assignmentService.updateAssignment(assignmentId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{assignmentId}")
    @Operation(
            summary = "Delete desk assignment",
            description = "Remove a desk assignment (soft delete - marks as inactive)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Assignment deleted"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        assignmentService.deleteAssignment(assignmentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "Get assignment details", description = "Get details of a specific assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment details"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<DeskAssignmentResponseDTO> getAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        DeskAssignmentResponseDTO response = assignmentService.getAssignmentById(assignmentId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/desk/{deskId}")
    @Operation(
            summary = "Get assignments for a desk",
            description = "Get all assignments (past and present) for a specific desk"
    )
    public ResponseEntity<Page<DeskAssignmentResponseDTO>> getAssignmentsByDesk(
            @Parameter(description = "Desk ID") @PathVariable UUID deskId,
            @PageableDefault(size = 20, sort = "startUtc", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        Page<DeskAssignmentResponseDTO> assignments = assignmentService.getAssignmentsByDesk(deskId, userId, pageable);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/department/{departmentId}")
    @Operation(
            summary = "Get assignments in department",
            description = "Get all assignments in a department (Department Admin or System Admin only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department assignments"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Page<DeskAssignmentResponseDTO>> getAssignmentsByDepartment(
            @Parameter(description = "Department ID") @PathVariable UUID departmentId,
            @PageableDefault(size = 20, sort = "startUtc", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        Page<DeskAssignmentResponseDTO> assignments = assignmentService.getAssignmentsByDepartment(departmentId, userId, pageable);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/me/current")
    @Operation(
            summary = "Get my current desk assignment",
            description = "Get the desk currently assigned to me (if any)"
    )
    public ResponseEntity<DeskAssignmentSummaryDTO> getMyCurrentAssignment(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        DeskAssignmentSummaryDTO assignment = assignmentService.getMyCurrentAssignment(userId);

        if (assignment == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get all my desk assignments",
            description = "Get all my desk assignments (past, present, and future)"
    )
    public ResponseEntity<List<DeskAssignmentResponseDTO>> getMyAssignments(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        List<DeskAssignmentResponseDTO> assignments = assignmentService.getMyAssignments(userId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/desk/{deskId}/available")
    @Operation(
            summary = "Check desk availability for assignment",
            description = "Check if a desk is available for assignment in a given time range"
    )
    public ResponseEntity<Boolean> checkDeskAvailability(
            @PathVariable UUID deskId,
            @RequestParam java.time.Instant startUtc,
            @RequestParam(required = false) java.time.Instant endUtc,
            Authentication authentication
    ) {
        boolean available = assignmentService.isDeskAvailableForAssignment(deskId, startUtc, endUtc);
        return ResponseEntity.ok(available);
    }
}
