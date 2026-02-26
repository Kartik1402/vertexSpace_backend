package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingResponse {

    private UUID id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional: Count of floors (if needed)
    private Long floorCount;
}
