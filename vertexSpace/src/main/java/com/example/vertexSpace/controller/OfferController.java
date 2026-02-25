
package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.waitlist.AcceptOfferResponseDTO;
import com.example.vertexSpace.dto.waitlist.OfferResponseDTO;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Waitlist Offers", description = "Waitlist offer management (10-minute acceptance window)")
@SecurityRequirement(name = "bearer-auth")
public class OfferController {

    private final OfferService offerService;
    private final AuthService authService;

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return authService.getUserIdByEmail(authentication.getName());
    }

    @GetMapping("/me/waitlist-offers")
    @Operation(
            summary = "Get my active offers",
            description = "Get all active offers waiting for your response. " +
                    "Offers expire after 10 minutes. Shows countdown timer via remainingSeconds."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Your active offers",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))
            )
    })
    public ResponseEntity<List<OfferResponseDTO>> getMyOffers(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        List<OfferResponseDTO> offers = offerService.getMyActiveOffers(userId);
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/waitlist-offers/{offerId}")
    @Operation(summary = "Get offer details", description = "Get details of a specific offer by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Offer details",
                    content = @Content(schema = @Schema(implementation = OfferResponseDTO.class))
            ),
            @ApiResponse(responseCode = "403", description = "Not your offer"),
            @ApiResponse(responseCode = "404", description = "Offer not found")
    })
    public ResponseEntity<OfferResponseDTO> getOfferDetails(
            @Parameter(description = "Offer ID (time block ID)")
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        OfferResponseDTO offer = offerService.getOfferDetails(offerId, userId);
        return ResponseEntity.ok(offer);
    }

    @PostMapping("/waitlist-offers/{offerId}/accept")
    @Operation(
            summary = "Accept waitlist offer",
            description = "Accept an offer and convert it to confirmed booking. " +
                    "This is a CRITICAL operation with pessimistic locking to prevent race conditions. " +
                    "Validates: authorization, expiry, state, and conflicts before creating booking."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Offer accepted, booking created",
                    content = @Content(schema = @Schema(implementation = AcceptOfferResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Offer already processed (accepted/declined)"),
            @ApiResponse(responseCode = "403", description = "Not your offer"),
            @ApiResponse(responseCode = "404", description = "Offer not found"),
            @ApiResponse(responseCode = "409", description = "Slot no longer available (conflict detected)"),
            @ApiResponse(responseCode = "410", description = "Offer expired")
    })
    public ResponseEntity<AcceptOfferResponseDTO> acceptOffer(
            @Parameter(description = "Offer ID to accept")
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        AcceptOfferResponseDTO response = offerService.acceptOffer(offerId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/waitlist-offers/{offerId}/decline")
    @Operation(
            summary = "Decline waitlist offer",
            description = "Explicitly decline an offer. " +
                    "Note: Offers auto-expire after 10 minutes, so this is optional. " +
                    "Next person in queue will be offered the slot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Offer declined"),
            @ApiResponse(responseCode = "400", description = "Offer already processed"),
            @ApiResponse(responseCode = "403", description = "Not your offer"),
            @ApiResponse(responseCode = "404", description = "Offer not found")
    })
    public ResponseEntity<Void> declineOffer(
            @Parameter(description = "Offer ID to decline")
            @PathVariable UUID offerId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        offerService.declineOffer(offerId, userId);
        return ResponseEntity.noContent().build();
    }
}
