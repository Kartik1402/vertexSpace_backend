package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.waitlist.WaitlistEntryRequestDTO;
import com.example.vertexSpace.dto.waitlist.WaitlistEntryResponseDTO;
import com.example.vertexSpace.enums.WaitlistStatus;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/waitlist-entries")
@RequiredArgsConstructor
@Tag(name = "Waitlist", description = "Waitlist management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final AuthService authService;

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return authService.getUserIdByEmail(authentication.getName());
    }

    @PostMapping
    @Operation(
            summary = "Join waitlist",
            description = "Join waitlist for a specific time slot when resource is unavailable. " +
                    "System will create an offer when slot becomes available (FIFO order)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Successfully joined waitlist",
                    content = @Content(schema = @Schema(implementation = WaitlistEntryResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request (slot in past, invalid time range)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Already in waitlist for this slot")
    })
    public ResponseEntity<WaitlistEntryResponseDTO> joinWaitlist(
            @Valid @RequestBody WaitlistEntryRequestDTO request,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        WaitlistEntryResponseDTO response = waitlistService.joinWaitlist(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
            summary = "Get waitlist entries",
            description = "Query waitlist entries with optional filters. " +
                    "Regular users can only see their own entries. " +
                    "Admins can see all entries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Waitlist entries retrieved",
                    content = @Content(schema = @Schema(implementation = WaitlistEntryResponseDTO.class))
            )
    })
    public ResponseEntity<List<WaitlistEntryResponseDTO>> getWaitlistEntries(
            @Parameter(description = "Filter by resource ID")
            @RequestParam(required = false) UUID resourceId,

            @Parameter(description = "Filter by user ID (admin only)")
            @RequestParam(required = false) UUID userId,

            @Parameter(description = "Filter by status (ACTIVE, FULFILLED, CANCELLED, EXPIRED)")
            @RequestParam(required = false) WaitlistStatus status,

            @Parameter(description = "Filter by start time (entries after this time)")
            @RequestParam(required = false) Instant startUtc,

            Authentication authentication
    ) {
        UUID currentUserId = currentUserId(authentication);
        List<WaitlistEntryResponseDTO> entries = waitlistService.getWaitlistEntries(
                resourceId, userId, status, startUtc, currentUserId
        );
        return ResponseEntity.ok(entries);
    }

    @DeleteMapping("/{entryId}")
    @Operation(
            summary = "Leave waitlist",
            description = "Remove yourself from waitlist. Can only cancel ACTIVE entries. " +
                    "If an offer is already created, it will be cancelled too."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successfully left waitlist"),
            @ApiResponse(responseCode = "403", description = "Not your waitlist entry"),
            @ApiResponse(responseCode = "404", description = "Waitlist entry not found"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel entry with this status")
    })
    public ResponseEntity<Void> leaveWaitlist(
            @Parameter(description = "Waitlist entry ID")
            @PathVariable UUID entryId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        waitlistService.leaveWaitlist(entryId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get my active waitlist entries",
            description = "Get all ACTIVE waitlist entries for current user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Your active waitlist entries",
                    content = @Content(schema = @Schema(implementation = WaitlistEntryResponseDTO.class))
            )
    })
    public ResponseEntity<List<WaitlistEntryResponseDTO>> getMyWaitlistEntries(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        List<WaitlistEntryResponseDTO> entries = waitlistService.getMyActiveEntries(userId);
        return ResponseEntity.ok(entries);
    }
}
