package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.availability.BestSlotRequestDTO;
import com.example.vertexSpace.dto.availability.BestSlotResponseDTO;
import com.example.vertexSpace.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Availability Controller
 *
 * Best-slot algorithm endpoint for finding available time slots
 *
 * Endpoint:
 * - GET /api/v1/availability/best-slots - Find top 5 earliest available slots
 */
@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Validated
@Tag(name = "Availability", description = "Resource availability and best-slot suggestions")
@SecurityRequirement(name = "bearer-auth")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/best-slots")
    @Operation(
            summary = "Find best available slots",
            description = """                
                Finds top 5 earliest available slots for given date and duration"""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Best slot suggestions (up to 5)",
                    content = @Content(schema = @Schema(implementation = BestSlotResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters (e.g., invalid date, duration out of range)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - missing or invalid JWT token"
            )
    })
    public ResponseEntity<BestSlotResponseDTO> findBestSlots(
            @Parameter(
                    description = "Target date in IST (local date, no timezone)",
                    example = "2025-01-15",
                    required = true
            )
            @RequestParam
            @NotNull(message = "Date is required")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateIst,

            @Parameter(
                    description = "How long you need the resource (minutes)",
                    example = "60",
                    required = true
            )
            @RequestParam
            @NotNull(message = "Duration is required")
            @Min(value = 15, message = "Minimum duration is 15 minutes")
            @Max(value = 480, message = "Maximum duration is 8 hours (480 minutes)")
            Integer durationMinutes,

            @Parameter(
                    description = "Filter by resource type (DESK, ROOM, PARKING_SPOT)",
                    example = "ROOM"
            )
            @RequestParam(required = false)
            String type,

            @Parameter(
                    description = "Filter by floor ID (UUID)",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam(required = false)
            UUID floorId,

            @Parameter(
                    description = "Filter by building ID (UUID)",
                    example = "550e8400-e29b-41d4-a716-446655440001"
            )
            @RequestParam(required = false)
            UUID buildingId,

            @Parameter(
                    description = "Filter by minimum capacity (only applies to ROOM type)",
                    example = "10"
            )
            @RequestParam(required = false)
            @Min(value = 1, message = "Capacity must be at least 1")
            Integer capacityMin,

            @Parameter(
                    description = "Filter by department ID - results will prioritize this department's resources",
                    example = "550e8400-e29b-41d4-a716-446655440002"
            )
            @RequestParam(required = false)
            UUID departmentId
    ) {
        // Build request DTO (WITHOUT featureIds - not supported in this system)
        BestSlotRequestDTO request = new BestSlotRequestDTO(
                dateIst,
                durationMinutes,
                type,
                floorId,
                capacityMin,
                buildingId,
                departmentId,
                null
        );

        BestSlotResponseDTO response = availabilityService.findBestSlots(request);
        return ResponseEntity.ok(response);
    }
}
