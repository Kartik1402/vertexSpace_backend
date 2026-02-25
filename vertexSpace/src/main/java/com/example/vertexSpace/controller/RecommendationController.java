package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.recommendation.RecommendationResponseDTO;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized resource recommendations")
@SecurityRequirement(name = "bearer-auth")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AuthService authService;

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return authService.getUserIdByEmail(authentication.getName());
    }

    @GetMapping("/recommendations")
    @Operation(
            summary = "Get personalized recommendations",
            description = "Get top 3 resources you book most frequently (last 30 days). " +
                    "Useful for dashboard 'Quick Book' section. " +
                    "Based on confirmed bookings only."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Your top 3 most-booked resources",
                    content = @Content(schema = @Schema(implementation = RecommendationResponseDTO.class))
            )
    })
    public ResponseEntity<RecommendationResponseDTO> getMyRecommendations(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        RecommendationResponseDTO recommendations = recommendationService.getMyRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
}
