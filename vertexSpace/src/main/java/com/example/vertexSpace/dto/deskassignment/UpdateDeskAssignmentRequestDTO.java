package com.example.vertexSpace.dto.deskassignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeskAssignmentRequestDTO {
    private Instant endUtc;

    private String notes;
}
