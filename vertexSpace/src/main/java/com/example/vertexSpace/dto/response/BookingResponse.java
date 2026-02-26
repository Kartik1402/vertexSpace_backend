package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.BlockType;
import com.example.vertexSpace.enums.ResourceType;
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
public class BookingResponse {

    private UUID id;

    // Resource details
    private UUID resourceId;
    private String resourceName;
    private ResourceType resourceType;

    // Location details
    private String buildingName;
    private String floorName;

    // User details
    private UUID userId;
    private String userDisplayName;
    private String userEmail;
    private String userDepartmentName;

    // Booking details
    private BlockType blockType;
    private BlockStatus status;
    private Instant startTime;
    private Instant endTime;
    private Instant conflictEndTime;  // end + buffer
    private Integer bufferMinutes;
    private String purpose;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
}
