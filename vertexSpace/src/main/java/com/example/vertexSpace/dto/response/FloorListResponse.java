package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of floors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorListResponse {

    private List<FloorResponse> floors;
    private Long totalCount;
    private Integer activeCount;
}
