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

import java.sql.Timestamp;
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

    private static Instant toInstant(Object v) {
        if (v == null) return null;
        if (v instanceof Instant i) return i;
        if (v instanceof Timestamp ts) return ts.toInstant();
        if (v instanceof java.util.Date d) return d.toInstant();
        if (v instanceof String s) {
            try { return Instant.parse(s); } catch (Exception ignored) { return null; }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public RecommendationResponseDTO getMyRecommendations(UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("Getting recommendations for user {}", currentUser.getEmail());

        final Instant now = Instant.now();
        final Instant since = now.minus(ANALYSIS_DAYS, ChronoUnit.DAYS);

        List<Object[]> results = blockRepo.findTopBookedResources(currentUser.getId(), since);

        List<RecommendationItemDTO> recommendations = new ArrayList<>();
        for (Object[] row : results) {
            if (recommendations.size() >= MAX_RECOMMENDATIONS) break;

            Instant lastBookedAt = toInstant(row.length > 4 ? row[4] : null);

            // Enforce: 0 <= (now - lastBookedAt) <= 30 days
            if (lastBookedAt == null) continue;
            if (lastBookedAt.isAfter(now)) continue;      // negative diff not allowed
            if (lastBookedAt.isBefore(since)) continue;   // older than 30 days not allowed

            recommendations.add(RecommendationItemDTO.builder()
                    .resourceId((UUID) row[0])
                    .resourceName((String) row[1])
                    .resourceType((String) row[2])
                    .bookingCount((Long) row[3])
                    .lastBookedAt(lastBookedAt)
                    .build());
        }

        return RecommendationResponseDTO.builder()
                .recommendations(recommendations)
                .period("Last " + ANALYSIS_DAYS + " days")
                .build();
    }
}
