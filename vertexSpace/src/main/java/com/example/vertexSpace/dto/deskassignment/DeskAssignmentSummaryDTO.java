package com.example.vertexSpace.dto.deskassignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeskAssignmentSummaryDTO {
    private UUID assignmentId;
    private String deskName;
    private String departmentName;
    private Boolean isIndefinite;
    private Boolean isCurrentlyActive;
    private java.time.Instant startUtc;
    private java.time.Instant endUtc;
}
