package com.example.vertexSpace.dto.deskassignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeskAssignmentResponseDTO {
    private UUID id;

    // Desk info
    private UUID deskId;
    private String deskName;
    private UUID departmentId;
    private String departmentName;

    // Assigned user info
    private UUID userId;
    private String userEmail;
    private String userDisplayName;

    // Assignment details
    private Instant startUtc;
    private Instant endUtc;
    private Boolean isIndefinite;
    private Boolean isCurrentlyActive;
    private Boolean isActive;
    private String notes;

    // Audit info
    private UUID assignedByUserId;
    private String assignedByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
