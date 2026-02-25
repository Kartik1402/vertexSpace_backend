package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.request.CreateBookingRequest;
import com.example.vertexSpace.dto.request.CreateRecurringBookingRequest;
import com.example.vertexSpace.dto.response.*;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.ResourceTimeBlock;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.service.OfferService;
import com.example.vertexSpace.enums.*;
import com.example.vertexSpace.exception.*;
import com.example.vertexSpace.dto.waitlist.WaitlistEntryResponseDTO;
import com.example.vertexSpace.repository.ResourceRepository;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.repository.WaitlistEntryRepository;
import com.example.vertexSpace.service.BookingService;
import com.example.vertexSpace.util.BookingValidator;
import com.example.vertexSpace.util.RecurrenceGenerator;
import com.example.vertexSpace.util.ResourceMapper;
import com.example.vertexSpace.service.WaitlistService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of BookingService
 * CRITICAL: Handles all booking logic with conflict detection and pessimistic locking
 * ENHANCED: Integrated with waitlist system for seamless conflict handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final ResourceTimeBlockRepository timeBlockRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final ResourceMapper mapper;
    private final BookingValidator bookingValidator;
    private final WaitlistService waitlistService;
    private final OfferService offerService;
    private final NotificationServiceImpl notificationService;
    private final RecurrenceGenerator recurrenceGenerator;

    // ========================================================================
    // CREATE BOOKING - ENHANCED WITH WAITLIST INTEGRATION
    // ========================================================================

    /**
     * Create booking with integrated waitlist conflict handling
     *
     * Flow:
     * 1. Validate user and time range
     * 2. Lock resource (pessimistic lock)
     * 3. Check for conflicts
     * 4. If conflict → Auto-join waitlist in SEPARATE transaction + throw exception with details
     * 5. If available → Create confirmed booking
     */
    @Override
    public BookingResponse createBooking(CreateBookingRequest request, UUID currentUserId) {
        log.info("Creating booking for resource {} by user {}", request.getResourceId(), currentUserId);

        // ============================================================
        // STEP 1-5: VALIDATION AND LOCKING
        // ============================================================
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        bookingValidator.validateBookingTime(request.getStartTime(), request.getEndTime());
        bookingValidator.validateBufferMinutes(request.getBufferMinutes());

        int bufferMinutes = request.getBufferMinutes() != null ? request.getBufferMinutes() : 15;
        Instant conflictEndTime = bookingValidator.calculateConflictEndTime(request.getEndTime(), bufferMinutes);

        Resource resource = timeBlockRepository.lockResourceForBooking(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (!resource.getIsActive()) {
            throw new ValidationException("Cannot book inactive resource");
        }
        if (resource.isAssignableDesk()) {
            throw new ValidationException(
                    "This desk is assigned to specific users and cannot be booked. " +
                            "Please contact your Department Admin for desk assignment."
            );
        }

        // Check user double-booking
        boolean userHasConflict = timeBlockRepository.userHasOverlappingBooking(
                currentUserId,
                request.getStartTime(),
                conflictEndTime
        );

        if (userHasConflict) {
            log.warn("User {} already has a booking during requested time", currentUserId);
            throw new DuplicateResourceException("You already have a booking during this time period");
        }

        // ============================================================
        // STEP 6: CHECK FOR RESOURCE CONFLICTS
        // ============================================================
        boolean hasConflict = timeBlockRepository.existsOverlappingBlock(
                request.getResourceId(),
                request.getStartTime(),
                conflictEndTime
        );

        if (hasConflict) {
            log.warn("Booking conflict detected for resource {}. Creating waitlist entry in separate transaction.",
                    request.getResourceId());

            // ============================================================
            // CRITICAL: Create pending booking + waitlist in SEPARATE transaction
            // This ensures they are saved even when we throw exception below
            // ============================================================
            WaitlistService.PendingBookingResult result =
                    waitlistService.createPendingBookingAndWaitlistEntry(
                            resource,
                            request,
                            user,
                            bufferMinutes,
                            conflictEndTime
                    );

            // At this point, pending booking and waitlist entry are COMMITTED to database
            log.info("Pending booking and waitlist entry saved successfully. Building conflict response.");

            // ============================================================
            // Build detailed conflict response
            // ============================================================
            BookingWithWaitlistResponse response = buildConflictResponseWithWaitlist(
                    resource,
                    request,
                    result,
                    bufferMinutes,
                    conflictEndTime
            );

            // ============================================================
            // Throw exception with response (data already saved!)
            // ============================================================
            throw new ResourceConflictException(
                    "Resource is not available. You've been automatically added to the waitlist.",
                    response
            );
        }

        // ============================================================
        // STEP 7: CREATE CONFIRMED BOOKING (NO CONFLICT)
        // ============================================================
        ResourceTimeBlock booking = new ResourceTimeBlock();
        booking.setResource(resource);
        booking.setUser(user);
        booking.setBlockType(BlockType.BOOKING);
        booking.setStatus(BlockStatus.CONFIRMED);
        booking.setStartTimeUtc(request.getStartTime());
        booking.setEndTimeUtc(request.getEndTime());
        booking.setConflictEndUtc(conflictEndTime);
        booking.setBufferMinutes(bufferMinutes);
        booking.setPurpose(request.getPurpose());

        ResourceTimeBlock saved = timeBlockRepository.save(booking);

        log.info("Booking created successfully: ID={}, Resource={}, User={}",
                saved.getId(), resource.getName(), user.getDisplayName());

        return mapper.toBookingResponse(saved);
    }

    // ========================================================================
    // WAITLIST INTEGRATION - HELPER METHODS
    // ========================================================================

    /**
     * Build conflict response with waitlist information
     * Called AFTER pending booking and waitlist entry are already saved
     */
    private BookingWithWaitlistResponse buildConflictResponseWithWaitlist(
            Resource resource,
            CreateBookingRequest request,
            WaitlistService.PendingBookingResult result,
            int bufferMinutes,
            Instant conflictEndTime) {

        try {
            // ============================================================
            // 1. GET CONFLICT DETAILS
            // ============================================================
            List<ResourceTimeBlock> conflicts = timeBlockRepository.findOverlappingBlocks(
                    request.getResourceId(),
                    request.getStartTime(),
                    conflictEndTime
            );

            // ============================================================
            // 2. FIND ALTERNATIVES (optional - can be empty list)
            // ============================================================
            List<Resource> alternatives = findAlternativeResources(
                    resource,
                    request.getStartTime(),
                    conflictEndTime
            );

            // ============================================================
            // 3. FIND NEXT AVAILABLE SLOTS (optional - can be empty list)
            // ============================================================
            List<BookingConflictResponseDTO.TimeSlotSuggestion> suggestedSlots =
                    findNextAvailableSlots(
                            resource,
                            request.getStartTime(),
                            request.getEndTime(),
                            bufferMinutes,
                            3
                    );

            // ============================================================
            // 4. BUILD USER-FRIENDLY MESSAGE
            // ============================================================
            String message = buildWaitlistAutoJoinMessage(
                    result.queuePosition(),
                    alternatives.size()
            );

            // ============================================================
            // 5. BUILD RESPONSE
            // ============================================================
            return BookingWithWaitlistResponse.builder()
                    .pendingBooking(mapper.toBookingResponse(result.pendingBooking()))
                    .waitlistEntry(WaitlistEntryResponseDTO.builder()
                            .id(result.waitlistEntry().getId())
                            .resourceId(resource.getId())
                            .resourceName(resource.getName())
                            .userId(result.waitlistEntry().getUser().getId())
                            .userDisplayName(result.waitlistEntry().getUser().getDisplayName())
                            .pendingBookingId(result.pendingBooking().getId())
                            .startUtc(request.getStartTime())
                            .endUtc(request.getEndTime())
                            .purpose(request.getPurpose())
                            .status(result.waitlistEntry().getStatus().name())
                            .queuePosition(result.queuePosition())
                            .createdAt(result.waitlistEntry().getCreatedAt())
                            .message("You're #" + result.queuePosition() + " in the waitlist queue")
                            .build())
                    .conflicts(conflicts.stream()
                            .map(this::toConflictingBookingDTO)
                            .collect(Collectors.toList()))
                    .alternativeResources(alternatives.stream()
                            .limit(5)
                            .map(this::toResourceResponseDTO)
                            .collect(Collectors.toList()))
                    .suggestedTimeSlots(suggestedSlots)
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("Error building conflict response", e);

            // Return minimal response (pending booking and waitlist already saved)
            return BookingWithWaitlistResponse.builder()
                    .pendingBooking(mapper.toBookingResponse(result.pendingBooking()))
                    .waitlistEntry(WaitlistEntryResponseDTO.builder()
                            .id(result.waitlistEntry().getId())
                            .resourceId(resource.getId())
                            .resourceName(resource.getName())
                            .pendingBookingId(result.pendingBooking().getId())
                            .status(result.waitlistEntry().getStatus().name())
                            .queuePosition(result.queuePosition())
                            .message("You're #" + result.queuePosition() + " in the waitlist queue")
                            .build())
                    .message("Resource is not available. You've been automatically added to the waitlist.")
                    .conflicts(List.of()).alternativeResources(List.of()).suggestedTimeSlots(List.of()).build();
        }
    }

    /**
     * Build user-friendly message for auto-join waitlist
     */
    private String buildWaitlistAutoJoinMessage(Integer queuePosition, int alternativesCount) {
        StringBuilder message = new StringBuilder();

        message.append("This resource is currently booked. ");
        message.append("You've been automatically added to the waitlist at position #")
                .append(queuePosition)
                .append(". ");

        if (alternativesCount > 0) {
            message.append("We've also found ")
                    .append(alternativesCount)
                    .append(" alternative resource(s) that may be available. ");
        }

        message.append("You'll be notified when your turn comes up!");

        return message.toString();
    }

    /**
     * Find alternative available resources similar to the requested one
     */
    private List<Resource> findAlternativeResources(
            Resource requestedResource,
            Instant startTime,
            Instant conflictEndTime) {

        try {
            return resourceRepository.findAvailableResourcesByFilters(
                    requestedResource.getResourceType(),
                    requestedResource.getFloor().getBuilding().getId(),
                    requestedResource.getCapacity(),
                    startTime,
                    conflictEndTime
            );
        } catch (Exception e) {
            log.warn("Error finding alternative resources", e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Find next available time slots for a specific resource
     */
    private List<BookingConflictResponseDTO.TimeSlotSuggestion> findNextAvailableSlots(
            Resource resource,
            Instant requestedStart,
            Instant requestedEnd,
            int bufferMinutes,
            int maxSuggestions) {

        List<BookingConflictResponseDTO.TimeSlotSuggestion> suggestions = new ArrayList<>();

        try {
            Duration requestedDuration = Duration.between(requestedStart, requestedEnd);
            Instant searchStart = requestedStart;
            Instant searchLimit = requestedStart.plus(Duration.ofDays(7));

            while (suggestions.size() < maxSuggestions && searchStart.isBefore(searchLimit)) {
                Instant slotEnd = searchStart.plus(requestedDuration);
                Instant conflictEnd = slotEnd.plus(Duration.ofMinutes(bufferMinutes));

                boolean isAvailable = !timeBlockRepository.existsOverlappingBlock(
                        resource.getId(),
                        searchStart,
                        conflictEnd
                );

                if (isAvailable) {
                    suggestions.add(BookingConflictResponseDTO.TimeSlotSuggestion.builder()
                            .startTimeUtc(searchStart)
                            .endTimeUtc(slotEnd)
                            .availability(calculateAvailabilityLabel(searchStart, requestedStart))
                            .build());
                }

                searchStart = searchStart.plus(Duration.ofHours(1));
            }
        } catch (Exception e) {
            log.warn("Error finding available slots", e);
        }

        return suggestions;
    }

    /**
     * Calculate human-readable availability label
     */
    private String calculateAvailabilityLabel(Instant slotTime, Instant requestedTime) {
        long hoursDiff = Duration.between(requestedTime, slotTime).toHours();

        if (hoursDiff < 2) return "Available soon";
        if (hoursDiff < 24) return "Available today";
        if (hoursDiff < 48) return "Available tomorrow";

        long daysDiff = hoursDiff / 24;
        return String.format("Available in %d %s", daysDiff, daysDiff == 1 ? "day" : "days");
    }

    /**
     * Helper to map block to conflict DTO
     */
    private BookingConflictResponseDTO.ConflictingBooking toConflictingBookingDTO(ResourceTimeBlock block) {
        return BookingConflictResponseDTO.ConflictingBooking.builder()
                .bookingId(block.getId())
                .startTimeUtc(block.getStartTimeUtc())
                .endTimeUtc(block.getEndTimeUtc())
                .bookedBy(block.getUser() != null ? block.getUser().getDisplayName() : "System")  // ✅ CORRECT FIELD NAME
                .build();
    }
    /**
     * Convert Resource entity to ResourceResponse
     */
    private ResourceResponse toResourceResponseDTO(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .resourceType(resource.getResourceType())
                .capacity(resource.getCapacity())
                .floorId(resource.getFloor().getId())
                .floorName(resource.getFloor().getFloorName())
                .buildingId(resource.getFloor().getBuilding().getId())
                .buildingName(resource.getFloor().getBuilding().getName())
                .isActive(resource.getIsActive())
                .build();
    }

    // ========================================================================
    // READ OPERATIONS
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, UUID currentUserId) {
        log.info("Fetching booking: {} by user: {}", bookingId, currentUserId);

        ResourceTimeBlock booking = timeBlockRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        validateBookingAccess(booking, currentUserId);

        return mapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getUserBookings(UUID userId) {
        log.info("Fetching all bookings for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<ResourceTimeBlock> bookings = timeBlockRepository.findUserBookings(userId);

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getUserUpcomingBookings(UUID userId) {
        log.info("Fetching upcoming bookings for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Instant now = Instant.now();
        List<ResourceTimeBlock> bookings = timeBlockRepository.findUserUpcomingBookings(userId, now);

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(bookings.size())
                .pastCount(0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getUserPastBookings(UUID userId) {
        log.info("Fetching past bookings for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Instant now = Instant.now();
        List<ResourceTimeBlock> bookings = timeBlockRepository.findUserPastBookings(userId, now);

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(0)
                .pastCount(bookings.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getResourceBookings(UUID resourceId, UUID currentUserId) {
        log.info("Fetching bookings for resource: {} by user: {}", resourceId, currentUserId);

        Resource resource = resourceRepository.findByIdWithRelations(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        validateResourceAccess(resource, currentUserId);

        List<ResourceTimeBlock> bookings = timeBlockRepository.findResourceBookings(resourceId);

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getResourceBookingsInRange(UUID resourceId, Instant startTime,
                                                          Instant endTime, UUID currentUserId) {
        log.info("Fetching bookings for resource {} from {} to {}", resourceId, startTime, endTime);

        Resource resource = resourceRepository.findByIdWithRelations(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        validateResourceAccess(resource, currentUserId);

        if (endTime.isBefore(startTime)) {
            throw new ValidationException("End time must be after start time");
        }

        List<ResourceTimeBlock> bookings = timeBlockRepository.findResourceBookingsInRange(
                resourceId, startTime, endTime
        );

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getAllBookings(UUID currentUserId) {
        log.info("Fetching all bookings (admin view) by user: {}", currentUserId);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        if (currentUser.getRole() != Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("Only system administrators can view all bookings");
        }

        List<ResourceTimeBlock> bookings = timeBlockRepository.findAllBookings();

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getDepartmentBookings(UUID departmentId, UUID currentUserId) {
        log.info("Fetching department bookings for dept {} by user {}", departmentId, currentUserId);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (!currentUser.getDepartment().getId().equals(departmentId)) {
                throw new AuthorizationException("Department admins can only view bookings for their own department");
            }
        } else if (currentUser.getRole() != Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("Only department or system administrators can view department bookings");
        }

        List<ResourceTimeBlock> bookings = timeBlockRepository.findDepartmentBookings(departmentId);

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

    // ========================================================================
    // DELETE OPERATION - CANCEL BOOKING
    // ========================================================================

    @Override
    @Transactional
    public BookingCancellationResponse cancelBooking(UUID bookingId, UUID currentUserId) {
        log.info("Cancelling booking: {} by user: {}", bookingId, currentUserId);

        ResourceTimeBlock booking = timeBlockRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        // Authorization check
        if (!booking.getUser().getId().equals(currentUserId) && currentUser.getRole() != Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BlockStatus.CANCELLED) {
            throw new ValidationException("Booking is already cancelled");
        }

        if (booking.getEndTimeUtc().isBefore(Instant.now())) {
            throw new ValidationException("Cannot cancel past bookings");
        }

        // Cancel booking
        Instant now = Instant.now();
        int updatedRows = timeBlockRepository.cancelBooking(bookingId, now);

        if (updatedRows == 0) {
            throw new DuplicateResourceException("Failed to cancel booking. It may have been already cancelled.");
        }

        log.info("Booking cancelled successfully: ID={}", bookingId);
        String timeSlot = formatTimeSlot(booking.getStartTimeUtc(), booking.getEndTimeUtc());
        notificationService.notifyBookingCancelled(
                currentUserId,
                bookingId,
                booking.getResource().getName(),
                timeSlot
        );


        // ============================================================
        // ✅ NEW: Trigger waitlist offer processing
        // ============================================================
        try {
            log.info("Checking waitlist for cancelled slot: resource={}", booking.getResource().getId());

            offerService.processWaitlistForCancelledBooking(
                    booking.getResource().getId(),
                    booking.getStartTimeUtc(),
                    booking.getEndTimeUtc()
            );

            log.info("Waitlist processing completed");
        } catch (Exception e) {
            log.error("Error processing waitlist after cancellation - cancellation still succeeded", e);
            // Don't fail the cancellation if waitlist processing fails
        }
        // ============================================================

        return BookingCancellationResponse.builder()
                .bookingId(bookingId)
                .message("Booking cancelled successfully. If anyone was waiting, they've been notified.")
                .cancelledAt(now)
                .resourceName(booking.getResource().getName())
                .originalStartTime(booking.getStartTimeUtc())
                .originalEndTime(booking.getEndTimeUtc())
                .build();
    }

    private String formatTimeSlot(Instant start, Instant end) {
        // Format: "Jan 15, 2PM-3PM"
        return start.toString() + " - " + end.toString(); // Improve formatting as needed
    }
    // ========================================================================
    // AUTHORIZATION HELPER METHODS
    // ========================================================================

    private void validateBookingAccess(ResourceTimeBlock booking, UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        if (booking.getUser().getId().equals(currentUserId)) {
            return;
        }

        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (booking.getResource().getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                return;
            }
        }

        throw new AuthorizationException("You do not have permission to view this booking");
    }

    private void validateResourceAccess(Resource resource, UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.DEPT_ADMIN) {
            if (resource.getOwningDepartment().getId().equals(currentUser.getDepartment().getId())) {
                return;
            }
        }

        log.debug("User {} accessing resource {} bookings for booking purposes",
                currentUserId, resource.getId());
    }
    // Add to class-level dependencies

// Update constructor (Lombok @RequiredArgsConstructor handles this automatically)

// ========================================================================
// CREATE RECURRING BOOKING
// ========================================================================

    @Override
    @Transactional
    public RecurringBookingResponse createRecurringBooking(
            CreateRecurringBookingRequest request,
            UUID currentUserId) {

        log.info("Creating recurring booking: pattern={}, resource={}, user={}",
                request.getPattern(), request.getResourceId(), currentUserId);

        // ===== STEP 1: VALIDATE USER & RESOURCE =====
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        bookingValidator.validateBookingTime(request.getStartTime(), request.getEndTime());
        bookingValidator.validateBufferMinutes(request.getBufferMinutes());

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + request.getResourceId()));

        if (!resource.getIsActive()) {
            throw new ValidationException("Cannot book inactive resource");
        }

        if (resource.isAssignableDesk()) {
            throw new ValidationException(
                    "This desk is assigned to specific users and cannot be booked. " +
                            "Please contact your Department Admin for desk assignment."
            );
        }

        // ===== STEP 2: GENERATE OCCURRENCES (in user's timezone) =====
        List<LocalDateTime> occurrencesIST = recurrenceGenerator.generateOccurrences(request);

        if (occurrencesIST.isEmpty()) {
            throw new ValidationException("No valid occurrences generated. Check your recurrence settings.");
        }

        log.info("Generated {} occurrence dates", occurrencesIST.size());

        // ===== STEP 3: CONVERT TO UTC & CHECK CONFLICTS =====
        UUID seriesId = UUID.randomUUID();
        Duration bookingDuration = Duration.between(request.getStartTime(), request.getEndTime());
        int bufferMinutes = request.getBufferMinutes() != null ? request.getBufferMinutes() : 15;

        List<BookingOccurrence> occurrences = new ArrayList<>();

        for (int i = 0; i < occurrencesIST.size(); i++) {
            LocalDateTime startIST = occurrencesIST.get(i);
            LocalDateTime endIST = startIST.plus(bookingDuration);

            Instant startUTC = recurrenceGenerator.toUTC(startIST, request.getTimezone());
            Instant endUTC = recurrenceGenerator.toUTC(endIST, request.getTimezone());
            Instant conflictEndUTC = endUTC.plusSeconds(bufferMinutes * 60L);

            // Check for resource conflicts
            boolean hasResourceConflict = timeBlockRepository.existsOverlappingBlock(
                    request.getResourceId(),
                    startUTC,
                    conflictEndUTC
            );

            // Check user double-booking
            boolean hasUserConflict = timeBlockRepository.userHasOverlappingBooking(
                    currentUserId,
                    startUTC,
                    conflictEndUTC
            );

            occurrences.add(BookingOccurrence.builder()
                    .occurrenceNumber(i + 1)
                    .startUTC(startUTC)
                    .endUTC(endUTC)
                    .conflictEndUTC(conflictEndUTC)
                    .hasResourceConflict(hasResourceConflict)
                    .hasUserConflict(hasUserConflict)
                    .build());
        }

        // ===== STEP 4: HANDLE CONFLICTS BASED ON STRATEGY =====
        List<BookingOccurrence> toCreate = occurrences.stream()
                .filter(occ -> !occ.hasResourceConflict && !occ.hasUserConflict)
                .toList();

        List<BookingOccurrence> conflicts = occurrences.stream()
                .filter(occ -> occ.hasResourceConflict || occ.hasUserConflict)
                .toList();

        // If strategy is FAIL_ON_CONFLICT and there are ANY conflicts, reject everything
        if (request.getConflictResolution() == ConflictResolution.FAIL_ON_CONFLICT && !conflicts.isEmpty()) {
            throw new ValidationException(
                    String.format("Cannot create recurring booking: %d out of %d occurrences have conflicts. " +
                                    "Change conflict resolution to SKIP_CONFLICTS to create available bookings only.",
                            conflicts.size(), occurrences.size())
            );
        }

        // ===== STEP 5: CREATE BOOKINGS (batch) =====
        List<ResourceTimeBlock> createdBookings = new ArrayList<>();

        for (BookingOccurrence occ : toCreate) {
            ResourceTimeBlock booking = new ResourceTimeBlock();
            booking.setResource(resource);
            booking.setUser(user);
            booking.setBlockType(BlockType.BOOKING);
            booking.setStatus(BlockStatus.CONFIRMED);
            booking.setStartTimeUtc(occ.startUTC);
            booking.setEndTimeUtc(occ.endUTC);
            booking.setConflictEndUtc(occ.conflictEndUTC);
            booking.setBufferMinutes(bufferMinutes);
            booking.setPurpose(request.getPurpose());

            // Recurring fields
            booking.setRecurringSeriesId(seriesId);
            booking.setIsRecurring(true);
            booking.setRecurrencePattern(request.getPattern());
            booking.setOccurrenceNumber(occ.occurrenceNumber);
            booking.setOriginalTimezone(request.getTimezone());

            createdBookings.add(booking);
        }

        // Batch save (single database call)
        List<ResourceTimeBlock> savedBookings = timeBlockRepository.saveAll(createdBookings);

        log.info("Recurring booking series created: seriesId={}, created={} out of {} (skipped {} conflicts)",
                seriesId, savedBookings.size(), occurrences.size(), conflicts.size());

        // ===== STEP 6: BUILD RESPONSE =====
        return RecurringBookingResponse.builder()
                .recurringSeriesId(seriesId)
                .pattern(request.getPattern())
                .timezone(request.getTimezone())
                .totalRequested(occurrences.size())
                .successfullyCreated(savedBookings.size())
                .skippedDueToConflicts(conflicts.size())
                .failedDueToErrors(0)
                .createdBookings(mapper.toBookingResponseList(savedBookings))
                .conflicts(buildConflictDetails(conflicts))
                .message(buildSuccessMessage(savedBookings.size(), conflicts.size(), occurrences.size()))
                .isPartialSuccess(!conflicts.isEmpty())
                .build();
    }

// ========================================================================
// GET RECURRING SERIES BOOKINGS
// ========================================================================

    @Override
    @Transactional(readOnly = true)
    public BookingListResponse getRecurringSeriesBookings(UUID seriesId, UUID currentUserId) {
        log.info("Fetching recurring series bookings: seriesId={}, user={}", seriesId, currentUserId);

        List<ResourceTimeBlock> bookings = timeBlockRepository.findByRecurringSeriesIdOrderByOccurrenceNumber(seriesId);

        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("No bookings found for series ID: " + seriesId);
        }

        // Verify user has access
        ResourceTimeBlock firstBooking = bookings.get(0);
        validateBookingAccess(firstBooking, currentUserId);

        Instant now = Instant.now();
        int upcomingCount = (int) bookings.stream()
                .filter(b -> b.getStartTimeUtc().isAfter(now))
                .count();

        int pastCount = (int) bookings.stream()
                .filter(b -> b.getEndTimeUtc().isBefore(now))
                .count();

        return BookingListResponse.builder()
                .bookings(mapper.toBookingResponseList(bookings))
                .totalCount((long) bookings.size())
                .upcomingCount(upcomingCount)
                .pastCount(pastCount)
                .build();
    }

// ========================================================================
// CANCEL RECURRING SERIES
// ========================================================================

    @Override
    @Transactional
    public RecurringSeriesCancellationResponse cancelRecurringSeries(UUID seriesId, UUID currentUserId) {
        log.info("Cancelling recurring series: seriesId={}, user={}", seriesId, currentUserId);

        // Find all future bookings in series
        Instant now = Instant.now();
        List<ResourceTimeBlock> futureBookings = timeBlockRepository
                .findByRecurringSeriesIdAndStartTimeAfter(seriesId, now);

        if (futureBookings.isEmpty()) {
            throw new ResourceNotFoundException("No future bookings found for series ID: " + seriesId);
        }

        // Verify user has access (check first booking)
        ResourceTimeBlock firstBooking = futureBookings.get(0);
        validateBookingAccess(firstBooking, currentUserId);

        // Cancel all future bookings
        List<UUID> cancelledIds = new ArrayList<>();
        for (ResourceTimeBlock booking : futureBookings) {
            int updated = timeBlockRepository.cancelBooking(booking.getId(), now);
            if (updated > 0) {
                cancelledIds.add(booking.getId());
            }
        }

        log.info("Cancelled {} future bookings in series {}", cancelledIds.size(), seriesId);

        return RecurringSeriesCancellationResponse.builder()
                .seriesId(seriesId)
                .cancelledCount(cancelledIds.size())
                .cancelledBookingIds(cancelledIds)
                .message(String.format("Successfully cancelled %d future bookings in this series", cancelledIds.size()))
                .cancelledAt(now)
                .build();
    }

// ========================================================================
// HELPER CLASSES & METHODS
// ========================================================================

    /**
     * Internal class to track each occurrence during creation
     */
    @Data
    @Builder
    private static class BookingOccurrence {
        private Integer occurrenceNumber;
        private Instant startUTC;
        private Instant endUTC;
        private Instant conflictEndUTC;
        private boolean hasResourceConflict;
        private boolean hasUserConflict;
    }

    /**
     * Build conflict details for response
     */
    private List<RecurringBookingResponse.ConflictDetail> buildConflictDetails(List<BookingOccurrence> conflicts) {
        return conflicts.stream()
                .map(occ -> RecurringBookingResponse.ConflictDetail.builder()
                        .attemptedStartTime(occ.startUTC)
                        .attemptedEndTime(occ.endUTC)
                        .occurrenceNumber(occ.occurrenceNumber)
                        .conflictReason(occ.hasUserConflict ?
                                "You have another booking at this time" :
                                "Resource already booked by someone else")
                        .build())
                .toList();
    }

    /**
     * Build user-friendly success message
     */
    private String buildSuccessMessage(int created, int skipped, int total) {
        if (skipped == 0) {
            return String.format("Successfully created all %d recurring bookings!", total);
        }
        return String.format("Successfully created %d out of %d bookings. %d dates were skipped due to conflicts. " +
                        "Check the 'conflicts' field for details.",
                created, total, skipped);
    }

}
