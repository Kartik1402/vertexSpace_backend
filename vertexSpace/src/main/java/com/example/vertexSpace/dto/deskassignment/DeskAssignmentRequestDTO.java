package com.example.vertexSpace.dto.deskassignment;

import jakarta.validation.constraints.NotNull;
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
public class DeskAssignmentRequestDTO {

    @NotNull(message = "Desk ID is required")
    private UUID deskId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Start date is required")
    private Instant startUtc;

    private Instant endUtc;

    private String notes;
}
