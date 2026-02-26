package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceStatisticsResponse {

    private UUID resourceId;
    private String resourceName;
    private Long totalBookings;
    private Long upcomingBookings;
    private Long pastBookings;
    private Double utilizationRate;  // Percentage (0-100)
}
