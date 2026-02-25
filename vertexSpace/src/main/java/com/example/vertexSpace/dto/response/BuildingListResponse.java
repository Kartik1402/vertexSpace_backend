package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of buildings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingListResponse {

    private List<BuildingResponse> buildings;
    private Long totalCount;
    private Integer activeCount;
}
