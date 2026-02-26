package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceListResponse {

    private List<ResourceResponse> resources;
    private Long totalCount;
    private Integer activeCount;

    // Statistics by resource type
    private Map<String, Long> countByType;  // e.g., {"ROOM": 10, "DESK": 25, "PARKING": 5}
}
