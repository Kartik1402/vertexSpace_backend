package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.recommendation.RecommendationItemDTO;
import com.example.vertexSpace.dto.recommendation.RecommendationResponseDTO;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import com.example.vertexSpace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ResourceTimeBlockRepository blockRepo;
    private final UserRepository userRepo;

    private static final int ANALYSIS_DAYS = 30;
    private static final int MAX_RECOMMENDATIONS = 3;

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional(readOnly = true)
    public RecommendationResponseDTO getMyRecommendations(UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("Getting recommendations for user {}", currentUser.getEmail());

        Instant since = Instant.now().minus(ANALYSIS_DAYS, ChronoUnit.DAYS);

        List<Object[]> results = blockRepo.findTopBookedResources(currentUser.getId(), since);

        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        for (Object[] row : results) {
            if (recommendations.size() >= MAX_RECOMMENDATIONS) break;

            recommendations.add(RecommendationItemDTO.builder()
                    .resourceId((UUID) row[0])
                    .resourceName((String) row[1])
                    .resourceType((String) row[2])
                    .bookingCount((Long) row[3])
                    .lastBookedAt((Instant) row[4])
                    .build());
        }

        return RecommendationResponseDTO.builder()
                .recommendations(recommendations)
                .period("Last " + ANALYSIS_DAYS + " days")
                .build();
    }
}
