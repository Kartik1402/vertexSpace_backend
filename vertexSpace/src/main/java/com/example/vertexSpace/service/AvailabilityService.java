package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.availability.BestSlotRequestDTO;
import com.example.vertexSpace.dto.availability.BestSlotResponseDTO;
import com.example.vertexSpace.dto.availability.SlotSuggestionDTO;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.enums.ResourceType;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.repository.ResourceRepository;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Availability Service
 *
 * Best-slot algorithm: Find top 5 earliest available slots for given criteria
 *
 * Algorithm:
 * 1. Validate request (date not in past, duration valid)
 * 2. Define search window (8 AM - 8 PM IST)
 * 3. Generate 15-minute candidate slots
 * 4. Get resources matching filters (NO feature filtering - not supported)
 * 5. For each candidate slot (earliest first):
 *    - Check each resource for availability
 *    - Include 15-min buffer in conflict detection
 * 6. Return first 5 available slots (greedy algorithm)
 *
 * Time Handling:
 * - User input: LocalDate in IST (no timezone)
 * - Search window: ZonedDateTime in IST
 * - Database queries: Instant in UTC
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final ResourceRepository resourceRepo;
    private final ResourceTimeBlockRepository blockRepo;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int SEARCH_START_HOUR = 8;  // 8 AM IST
    private static final int SEARCH_END_HOUR = 20;   // 8 PM IST
    private static final int SLOT_INCREMENT_MINUTES = 15;
    private static final int BUFFER_MINUTES = 15;
    private static final int MAX_SUGGESTIONS = 5;

    /**
     * Find best available slots (top 5 earliest)
     *
     * GET /api/v1/availability/best-slots
     *
     * @param request Search criteria (date, duration, optional filters)
     * @return Response with up to 5 earliest available slots
     * @throws ValidationException if date is in past or duration invalid
     */
    @Transactional(readOnly = true)
    public BestSlotResponseDTO findBestSlots(BestSlotRequestDTO request) {
        log.info("Finding best slots for date {} duration {} minutes with filters: type={}, floor={}, building={}, dept={}, capacity={}",
                request.getDateIst(),
                request.getDurationMinutes(),
                request.getType(),
                request.getFloorId(),
                request.getBuildingId(),
                request.getDepartmentId(),
                request.getCapacityMin()
        );

        // STEP 1: Validate request
        validateRequest(request);

        // STEP 2: Define search window in IST
        ZonedDateTime windowStart = request.getDateIst()
                .atTime(SEARCH_START_HOUR, 0)
                .atZone(IST_ZONE);

        ZonedDateTime windowEnd = request.getDateIst()
                .atTime(SEARCH_END_HOUR, 0)
                .atZone(IST_ZONE);

        log.debug("Search window: {} to {} IST", windowStart, windowEnd);

        // STEP 3: Generate candidate start times (15-min increments)
        List<ZonedDateTime> candidates = generateCandidateSlots(
                windowStart,
                windowEnd,
                request.getDurationMinutes()
        );

        log.debug("Generated {} candidate slots", candidates.size());

        if (candidates.isEmpty()) {
            log.warn("No candidate slots available for duration {} minutes", request.getDurationMinutes());
            return buildEmptyResponse(request, "Duration too long for search window");
        }

        // STEP 4: Get matching resources (NO FEATURE FILTERING)
        ResourceType resourceType = null;
        if (request.getType() != null) {
            try {
                resourceType = ResourceType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid resource type: " + request.getType() +
                        ". Valid types: DESK, ROOM, PARKING_SPOT");
            }
        }

        List<Resource> resources = resourceRepo.findByFilters(
                resourceType,              // ✅ ResourceType enum (not String)
                request.getFloorId(),
                request.getBuildingId(),
                request.getDepartmentId(),
                request.getCapacityMin()
                // ❌ NO featureIds parameter - features not supported
        );

        log.debug("Found {} matching resources", resources.size());

        if (resources.isEmpty()) {
            log.warn("No resources match the specified filters");
            return buildEmptyResponse(request, "No resources match your filters");
        }

        // STEP 5: Find available slots (earliest first, greedy algorithm)
        List<SlotSuggestionDTO> suggestions = findAvailableSlots(
                resources,
                candidates,
                request.getDurationMinutes()
        );

        log.info("Found {} available slots out of {} candidates",
                suggestions.size(), candidates.size());

        // Build response
        return BestSlotResponseDTO.builder()
                .dateIst(request.getDateIst())
                .searchWindow(SEARCH_START_HOUR + ":00-" + SEARCH_END_HOUR + ":00 IST")
                .durationMinutes(request.getDurationMinutes())
                .totalSuggestions(suggestions.size())
                .suggestions(suggestions)
                .build();
    }

    /**
     * Validate request parameters
     */
    private void validateRequest(BestSlotRequestDTO request) {
        // Check date not in past
        LocalDate today = LocalDate.now(IST_ZONE);
        if (request.getDateIst().isBefore(today)) {
            throw new ValidationException("Cannot search for slots in the past. Date must be today or later.");
        }

        // Check duration
        if (request.getDurationMinutes() < 15) {
            throw new ValidationException("Duration must be at least 15 minutes");
        }
        if (request.getDurationMinutes() > 480) {
            throw new ValidationException("Duration cannot exceed 8 hours (480 minutes)");
        }

        // Check if duration fits in search window
        int availableMinutes = (SEARCH_END_HOUR - SEARCH_START_HOUR) * 60;
        if (request.getDurationMinutes() > availableMinutes) {
            throw new ValidationException(
                    String.format("Duration %d minutes exceeds search window (%d hours)",
                            request.getDurationMinutes(), (SEARCH_END_HOUR - SEARCH_START_HOUR))
            );
        }
    }

    /**
     * Generate candidate slot start times (15-minute increments)
     *
     * Only generates slots where the entire duration fits before window end
     */
    private List<ZonedDateTime> generateCandidateSlots(
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            int durationMinutes
    ) {
        List<ZonedDateTime> candidates = new ArrayList<>();
        ZonedDateTime current = windowStart;

        // Calculate latest possible start time (duration must fit before window end)
        ZonedDateTime latestStart = windowEnd.minusMinutes(durationMinutes);

        while (!current.isAfter(latestStart)) {
            candidates.add(current);
            current = current.plusMinutes(SLOT_INCREMENT_MINUTES);
        }

        return candidates;
    }

    /**
     * Find available slots using greedy algorithm
     *
     * Nested loop optimization:
     * - Outer loop: Candidate slots (earliest first)
     * - Inner loop: Resources
     * - Early exit: Stop when MAX_SUGGESTIONS found
     *
     * This ensures we return the 5 EARLIEST available slots
     */
    private List<SlotSuggestionDTO> findAvailableSlots(
            List<Resource> resources,
            List<ZonedDateTime> candidates,
            int durationMinutes
    ) {
        List<SlotSuggestionDTO> results = new ArrayList<>();
        int checkedSlots = 0;

        // Outer loop: Candidate slots (earliest first)
        for (ZonedDateTime istStart : candidates) {
            checkedSlots++;

            // Inner loop: Resources
            for (Resource resource : resources) {
                // Convert IST to UTC for database query
                Instant utcStart = istStart.toInstant();
                Instant utcEnd = utcStart.plus(durationMinutes, ChronoUnit.MINUTES);
                Instant bufferEnd = utcEnd.plus(BUFFER_MINUTES, ChronoUnit.MINUTES);

                // Check availability (including buffer)
                boolean isAvailable = !blockRepo.hasConflict(
                        resource.getId(),
                        utcStart,
                        bufferEnd
                );

                if (isAvailable) {
                    // Add suggestion
                    ZonedDateTime istEnd = istStart.plusMinutes(durationMinutes);

                    results.add(SlotSuggestionDTO.builder()
                            .resourceId(resource.getId())
                            .resourceName(resource.getName())
                            .resourceType(resource.getResourceType().name())
                            .startUtc(utcStart)
                            .endUtc(utcEnd)
                            .startIst(istStart)
                            .endIst(istEnd)
                            .capacity(resource.getCapacity())
                            .floorName(resource.getFloor() != null ?
                                    resource.getFloor().getFloorName() : null)
                            .buildingName(resource.getFloor() != null &&
                                    resource.getFloor().getBuilding() != null ?
                                    resource.getFloor().getBuilding().getName() : null)
                            .build()
                    );

                    log.debug("Found available slot: {} at {} IST (slot #{}/{})",
                            resource.getName(), istStart, results.size(), MAX_SUGGESTIONS
                    );

                    // Early exit: Stop when we have enough suggestions
                    if (results.size() >= MAX_SUGGESTIONS) {
                        log.info("Reached max suggestions ({}), checked {} out of {} candidate slots",
                                MAX_SUGGESTIONS, checkedSlots, candidates.size());
                        return results;
                    }
                }
            }
        }

        log.info("Checked all {} candidate slots, found {} available slots",
                checkedSlots, results.size());
        return results;
    }

    /**
     * Build empty response when no resources match filters or no slots available
     */
    private BestSlotResponseDTO buildEmptyResponse(BestSlotRequestDTO request, String reason) {
        return BestSlotResponseDTO.builder()
                .dateIst(request.getDateIst())
                .searchWindow(SEARCH_START_HOUR + ":00-" + SEARCH_END_HOUR + ":00 IST")
                .durationMinutes(request.getDurationMinutes())
                .totalSuggestions(0)
                .suggestions(new ArrayList<>())
                .build();
    }
}
