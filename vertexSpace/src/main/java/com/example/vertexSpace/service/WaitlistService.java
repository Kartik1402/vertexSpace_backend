package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.waitlist.WaitlistEntryRequestDTO;
import com.example.vertexSpace.dto.waitlist.WaitlistEntryResponseDTO;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.ResourceTimeBlock;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.entity.WaitlistEntry;
import com.example.vertexSpace.dto.request.CreateBookingRequest;
import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.BlockType;
import org.springframework.transaction.annotation.Propagation;
import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.WaitlistStatus;
import com.example.vertexSpace.exception.AuthorizationException;
import com.example.vertexSpace.exception.ConflictException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.exception.ValidationException;
import com.example.vertexSpace.repository.ResourceRepository;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.vertexSpace.enums.BlockStatus.DECLINED;

/**
 * Waitlist Service
 *
 * Handles:
 * - Joining waitlist (manual + auto-join from booking conflicts)
 * - Leaving waitlist (cancels associated PENDING_WAITLIST booking)
 * - Viewing waitlist entries
 * - Queue position calculation
 * - Processing offers (accept/decline)
 * - Managing pending bookings lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistEntryRepository waitlistRepo;
    private final ResourceRepository resourceRepo;
    private final ResourceTimeBlockRepository blockRepo;
    private final OfferService offerService;
    private final UserRepository userRepo;

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Join waitlist for unavailable slot (MANUAL join)
     *
     * NOTE: Most users will join waitlist automatically when booking fails.
     * This method is for users who want to manually join waitlist without
     * attempting to book first.
     */
    public WaitlistEntryResponseDTO joinWaitlist(
            WaitlistEntryRequestDTO request,
            UUID currentUserId
    ) {

        User currentUser = requireUser(currentUserId);
        log.info("User {} manually joining waitlist for resource {} ({} to {})",
                currentUser.getEmail(),
                request.getResourceId(),
                request.getStartUtc(),
                request.getEndUtc()

        );
// 1. Validate resource exists
        Resource resource = resourceRepo.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found: " + request.getResourceId()
                ));

        if (!resource.getIsActive()) {
            throw new ValidationException("Resource is not active");
        }

// 2. Validate time range

        if (request.getStartUtc().isBefore(Instant.now())) {
            throw new ValidationException("Cannot join waitlist for past time slots");
        }

        if (!request.getEndUtc().isAfter(request.getStartUtc())) {
            throw new ValidationException("End time must be after start time");

        }

// 3. Check if user already in waitlist for this slot

        boolean alreadyInWaitlist = waitlistRepo.existsByResourceIdAndStartUtcAndEndUtcAndUserIdAndStatus(
                request.getResourceId(),
                request.getStartUtc(),
                request.getEndUtc(),
                currentUser.getId(),
                WaitlistStatus.ACTIVE

        );

        if (alreadyInWaitlist) {
            throw new ConflictException("You're already in waitlist for this slot");
        }

// 4. Verify slot is actually unavailable (Include 15-min buffer)

        Instant bufferEnd = request.getEndUtc().plusSeconds(15 * 60);
        boolean hasConflict = blockRepo.hasConflict(
                request.getResourceId(),
                request.getStartUtc(),
                bufferEnd

        );

        if (!hasConflict) {
            throw new ValidationException(
                    "Slot is available. Please book directly instead of joining waitlist."

            );
        }

// 5. Calculate queue position BEFORE creating entry

        Integer currentQueueLength = waitlistRepo.countActiveEntriesForResource(
                request.getResourceId()

        );

        Integer newQueuePosition = currentQueueLength + 1;

// 6. Create PENDING_WAITLIST booking

        ResourceTimeBlock pendingBooking = new ResourceTimeBlock();

        pendingBooking.setResource(resource);
        pendingBooking.setUser(currentUser);
        pendingBooking.setBlockType(com.example.vertexSpace.enums.BlockType.BOOKING);
        pendingBooking.setStatus(BlockStatus.PENDING_WAITLIST);
        pendingBooking.setStartTimeUtc(request.getStartUtc());
        pendingBooking.setEndTimeUtc(request.getEndUtc());
        pendingBooking.setConflictEndUtc(bufferEnd);
        pendingBooking.setBufferMinutes(15);
        pendingBooking.setPurpose(request.getPurpose());
        ResourceTimeBlock savedBooking = blockRepo.save(pendingBooking);
        log.info("Created PENDING_WAITLIST booking: {}", savedBooking.getId());

// 7. Create waitlist entry linked to pending booking

        WaitlistEntry entry = WaitlistEntry.builder()
                .resource(resource)
                .user(currentUser)
                .pendingBooking(savedBooking)
                .startUtc(request.getStartUtc())
                .endUtc(request.getEndUtc())
                .purpose(request.getPurpose())
                .status(WaitlistStatus.ACTIVE)
                .queuePosition(newQueuePosition)
                .build();
        WaitlistEntry saved = waitlistRepo.save(entry);
        log.info("Waitlist entry created: ID={}, Position={}", saved.getId(), newQueuePosition);
        return WaitlistEntryResponseDTO.builder()
                .id(saved.getId())
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .userId(currentUser.getId())
                .userEmail(currentUser.getEmail())
                .userDisplayName(currentUser.getDisplayName())
                .pendingBookingId(savedBooking.getId())
                .startUtc(saved.getStartUtc())
                .endUtc(saved.getEndUtc())
                .purpose(saved.getPurpose())
                .status(saved.getStatus().name())
                .queuePosition(newQueuePosition)
                .createdAt(saved.getCreatedAt())
                .message("You're #" + newQueuePosition + " in the waitlist queue")
                .build();

    }

    /**
     * Leave waitlist (cancel entry)
     * Also deletes the associated PENDING_WAITLIST booking
     */
    @Transactional
    public void leaveWaitlist(UUID entryId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("User {} leaving waitlist entry {}", currentUser.getEmail(), entryId);

        WaitlistEntry entry = waitlistRepo.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Waitlist entry not found: " + entryId
                ));

        // Authorization check
        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Not your waitlist entry");
        }

        // Can only cancel ACTIVE or OFFERED entries
        if (entry.getStatus() != WaitlistStatus.ACTIVE &&
                entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new ValidationException(
                    "Cannot cancel waitlist entry with status: " + entry.getStatus()
            );
        }

        // Update status
        entry.setStatus(WaitlistStatus.CANCELLED);
        entry.setFulfilledAt(Instant.now());
        entry.setQueuePosition(null);  // No longer in queue
        waitlistRepo.save(entry);

        log.info("Waitlist entry {} cancelled", entryId);

        // Delete associated PENDING_WAITLIST booking
        if (entry.getPendingBooking() != null) {
            UUID bookingId = entry.getPendingBooking().getId();
            blockRepo.deleteById(bookingId);
            log.info("Deleted PENDING_WAITLIST booking: {}", bookingId);
        }

        // If there's an active offer for this entry, cancel it too
        offerService.cancelOfferByWaitlistEntry(entryId);

        // Recalculate queue positions for remaining entries
        recalculateQueuePositions(entry.getResource().getId());
    }

    /**
     * Accept waitlist offer
     * Converts PENDING_WAITLIST booking to CONFIRMED
     */
    @Transactional
    public void acceptOffer(UUID entryId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("User {} accepting offer for waitlist entry {}",
                currentUser.getEmail(), entryId);

        WaitlistEntry entry = waitlistRepo.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Waitlist entry not found: " + entryId
                ));

        // Authorization check
        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Not your waitlist entry");
        }

        // Must be in OFFERED status
        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new ValidationException(
                    "Cannot accept offer for entry with status: " + entry.getStatus()
            );
        }

        // Check if offer expired
        if (entry.isOfferExpired()) {
            throw new ValidationException("Offer has expired");
        }

        // Convert PENDING_WAITLIST booking to CONFIRMED
        ResourceTimeBlock pendingBooking = entry.getPendingBooking();
        if (pendingBooking == null) {
            throw new IllegalStateException(
                    "No pending booking found for waitlist entry: " + entryId
            );
        }

        pendingBooking.setStatus(BlockStatus.CONFIRMED);
        blockRepo.save(pendingBooking);
        log.info("Converted booking {} from PENDING_WAITLIST to CONFIRMED",
                pendingBooking.getId());

        // Update waitlist entry
        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setFulfilledAt(Instant.now());
        entry.setQueuePosition(null);  // No longer in queue
        waitlistRepo.save(entry);

        log.info("Waitlist offer accepted: entry={}, booking={}",
                entryId, pendingBooking.getId());

        // Recalculate queue positions
        recalculateQueuePositions(entry.getResource().getId());
    }

    /**
     * Decline waitlist offer
     * Deletes PENDING_WAITLIST booking and processes next in queue
     */
    @Transactional
    public void declineOffer(UUID entryId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("User {} declining offer for waitlist entry {}",
                currentUser.getEmail(), entryId);

        WaitlistEntry entry = waitlistRepo.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Waitlist entry not found: " + entryId
                ));

        // Authorization check
        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Not your waitlist entry");
        }

        // Must be in OFFERED status
        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new ValidationException(
                    "Cannot decline offer for entry with status: " + entry.getStatus()
            );
        }

        // Delete PENDING_WAITLIST booking
        if (entry.getPendingBooking() != null) {
            UUID bookingId = entry.getPendingBooking().getId();
            blockRepo.deleteById(bookingId);
            log.info("Deleted PENDING_WAITLIST booking: {}", bookingId);
        }

        // Update waitlist entry
        entry.setStatus(WaitlistStatus.CANCELLED);
        entry.setFulfilledAt(Instant.now());
        entry.setQueuePosition(null);  // No longer in queue
        waitlistRepo.save(entry);

        log.info("Waitlist offer declined: {}", entryId);

        // Process next person in queue
        processNextInWaitlist(
                entry.getResource().getId(),
                entry.getStartUtc(),
                entry.getEndUtc()
        );
    }

    /**
     * Get waitlist entries with filters
     *
     * Authorization:
     * - USER: Can only see own entries
     * - ADMIN: Can see all entries
     */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponseDTO> getWaitlistEntries(
            UUID resourceId,
            UUID userId,
            WaitlistStatus status,
            Instant startUtc,
            UUID currentUserId
    ) {
        User currentUser = requireUser(currentUserId);

        // Apply authorization filter
        boolean isAdmin = currentUser.getRole() != null &&
                currentUser.getRole().name().contains("ADMIN");
        if (!isAdmin) {
            userId = currentUser.getId(); // Force filter to current user
        }

        List<WaitlistEntry> entries = waitlistRepo.findWithFilters(
                userId,
                resourceId,
                status,
                startUtc
        );

        return entries.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user's active waitlist entries
     */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponseDTO> getMyActiveEntries(UUID currentUserId) {
        requireUser(currentUserId);

        List<WaitlistEntry> entries = waitlistRepo.findByUserIdAndStatusOrderByCreatedAtDesc(
                currentUserId,
                WaitlistStatus.ACTIVE
        );

        return entries.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user's pending (OFFERED) waitlist entries
     */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponseDTO> getMyPendingOffers(UUID currentUserId) {
        requireUser(currentUserId);

        List<WaitlistEntry> entries = waitlistRepo.findByUserIdAndStatusOrderByCreatedAtDesc(
                currentUserId,
                WaitlistStatus.OFFERED
        );

        return entries.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Process next person in waitlist (called when slot becomes available)
     */
    @Transactional
    public void processNextInWaitlist(UUID resourceId, Instant startUtc, Instant endUtc) {
        log.info("Processing waitlist for resource {} ({} to {})",
                resourceId, startUtc, endUtc
        );

        var nextEntry = waitlistRepo.findNextInQueue(
                resourceId,
                startUtc,
                endUtc,
                WaitlistStatus.ACTIVE
        );

        if (nextEntry.isEmpty()) {
            log.info("No one in waitlist for this slot");
            return;
        }

        WaitlistEntry entry = nextEntry.get();
        log.info("Next in queue: {} (entry {})",
                entry.getUser().getEmail(),
                entry.getId()
        );

        // Create offer
        offerService.createOffer(entry);

        // Update waitlist entry status
        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setOfferedAt(Instant.now());
        entry.setOfferExpiresAt(Instant.now().plusSeconds(15 * 60)); // 15 minutes
        waitlistRepo.save(entry);

        log.info("Offer created for waitlist entry {}, expires at {}",
                entry.getId(), entry.getOfferExpiresAt());
    }

    /**
     * Expire old waitlist entries (called by scheduler)
     * Handles:
     * 1. Entries for past time slots
     * 2. Expired offers that weren't accepted
     */
    @Transactional
    public void expireOldEntries() {
        Instant now = Instant.now();

        // 1. Expire entries for past time slots
        List<WaitlistEntry> pastEntries = waitlistRepo.findActiveEntriesForPastTimeSlots(now);

        for (WaitlistEntry entry : pastEntries) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            entry.setFulfilledAt(now);
            entry.setQueuePosition(null);

            // Delete pending booking
            if (entry.getPendingBooking() != null) {
                blockRepo.deleteById(entry.getPendingBooking().getId());
                log.info("Deleted expired PENDING_WAITLIST booking: {}",
                        entry.getPendingBooking().getId());
            }
        }

        if (!pastEntries.isEmpty()) {
            waitlistRepo.saveAll(pastEntries);
            log.info("Expired {} waitlist entries for past time slots", pastEntries.size());
        }

        // 2. Expire offers that weren't accepted in time
        List<WaitlistEntry> expiredOffers = waitlistRepo.findExpiredOffers(now);

        for (WaitlistEntry entry : expiredOffers) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            entry.setFulfilledAt(now);
            entry.setQueuePosition(null);

            // Delete pending booking
            if (entry.getPendingBooking() != null) {
                blockRepo.deleteById(entry.getPendingBooking().getId());
                log.info("Deleted expired offer booking: {}",
                        entry.getPendingBooking().getId());
            }

            // Process next in queue
            processNextInWaitlist(
                    entry.getResource().getId(),
                    entry.getStartUtc(),
                    entry.getEndUtc()
            );
        }

        if (!expiredOffers.isEmpty()) {
            waitlistRepo.saveAll(expiredOffers);
            log.info("Expired {} offers that weren't accepted in time", expiredOffers.size());
        }
    }

    /**
     * Recalculate queue positions for all ACTIVE entries for a resource
     * Called after someone leaves queue or offer is fulfilled/declined
     */
    @Transactional
    public void recalculateQueuePositions(UUID resourceId) {
        log.debug("Recalculating queue positions for resource {}", resourceId);

        List<WaitlistEntry> activeEntries = waitlistRepo.findByResourceIdAndStatusOrderByCreatedAt(
                resourceId,
                WaitlistStatus.ACTIVE
        );

        int position = 1;
        for (WaitlistEntry entry : activeEntries) {
            entry.setQueuePosition(position++);
        }

        waitlistRepo.saveAll(activeEntries);
        log.debug("Recalculated {} queue positions", activeEntries.size());
    }

    /**
     * Map entity to response DTO
     */
    private WaitlistEntryResponseDTO mapToResponseDTO(WaitlistEntry entry) {
        return WaitlistEntryResponseDTO.builder()
                .id(entry.getId())
                .resourceId(entry.getResource().getId())
                .resourceName(entry.getResource().getName())
                .userId(entry.getUser().getId())
                .userEmail(entry.getUser().getEmail())
                .userDisplayName(entry.getUser().getDisplayName())
                .pendingBookingId(entry.getPendingBooking() != null ?
                        entry.getPendingBooking().getId() : null)
                .startUtc(entry.getStartUtc())
                .endUtc(entry.getEndUtc())
                .purpose(entry.getPurpose())
                .status(entry.getStatus().name())
                .queuePosition(entry.getQueuePosition())
                .offerExpiresAt(entry.getOfferExpiresAt())
                .createdAt(entry.getCreatedAt())
                .offeredAt(entry.getOfferedAt())
                .fulfilledAt(entry.getFulfilledAt())
                .message(buildStatusMessage(entry))
                .build();
    }

    /**
     * Build user-friendly status message
     */
    private String buildStatusMessage(WaitlistEntry entry) {
        return switch (entry.getStatus()) {
            case ACTIVE -> entry.getQueuePosition() != null ?
                    "You're #" + entry.getQueuePosition() + " in the queue" :
                    "Active in waitlist";
            case OFFERED -> {
                Long minutesLeft = entry.getMinutesUntilExpiry();
                yield minutesLeft != null ?
                        "Offer expires in " + minutesLeft + " minutes" :
                        "You have an offer pending";
            }

            case EXPIRED -> "Offer expired or time slot passed";
            case CANCELLED -> "Cancelled by user";
        };
    }
    /**
     * Create pending booking and waitlist entry in a SEPARATE transaction
     *
     * CRITICAL: Uses REQUIRES_NEW propagation to ensure data is saved
     * even when the calling method (BookingServiceImpl.createBooking)
     * throws ResourceConflictException and rolls back its transaction.
     *
     * @param resource The resource being requested
     * @param request The booking request
     * @param user The user making the request
     * @param bufferMinutes Buffer minutes
     * @param conflictEndTime Calculated conflict end time
     * @return The saved pending booking and waitlist entry
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PendingBookingResult createPendingBookingAndWaitlistEntry(
            Resource resource,
            CreateBookingRequest request,
            User user,
            int bufferMinutes,
            Instant conflictEndTime) {

        log.info("Creating PENDING_WAITLIST booking and waitlist entry for user {} on resource {}",
                user.getId(), resource.getId());

        // ============================================================
        // 1. CREATE PENDING_WAITLIST BOOKING
        // ============================================================
//        ResourceTimeBlock pendingBooking = new ResourceTimeBlock();
//        pendingBooking.setResource(resource);
//        pendingBooking.setUser(user);
//        pendingBooking.setBlockType(BlockType.BOOKING);
//        pendingBooking.setStatus(BlockStatus.PENDING_WAITLIST);
//        pendingBooking.setStartTimeUtc(request.getStartTime());
//        pendingBooking.setEndTimeUtc(request.getEndTime());
//        pendingBooking.setConflictEndUtc(conflictEndTime);
//        pendingBooking.setBufferMinutes(bufferMinutes);
//        pendingBooking.setPurpose(request.getPurpose());
//
//        ResourceTimeBlock savedBooking = blockRepo.save(pendingBooking);
//        log.info("Saved PENDING_WAITLIST booking: ID={}", savedBooking.getId());

        // ============================================================
        // 2. GET QUEUE POSITION
        // ============================================================
        Integer currentQueueLength = waitlistRepo.countActiveEntriesForResource(
                request.getResourceId());
        Integer newQueuePosition = currentQueueLength + 1;

        // ============================================================
        // 3. CREATE WAITLIST ENTRY
        // ============================================================
        WaitlistEntry waitlistEntry = WaitlistEntry.builder()
                .resource(resource)
                .user(user)
                .startUtc(request.getStartTime())
                .endUtc(request.getEndTime())
                .purpose(request.getPurpose())
                .status(WaitlistStatus.ACTIVE)
                .queuePosition(newQueuePosition)
                .build();

        WaitlistEntry savedWaitlist = waitlistRepo.save(waitlistEntry);
        log.info("Saved waitlist entry: ID={}, Position={}",
                savedWaitlist.getId(), newQueuePosition);

        // ============================================================
        // 4. RETURN RESULT (TRANSACTION COMMITS HERE)
        // ============================================================
        return new PendingBookingResult(null, savedWaitlist, newQueuePosition);
    }

    /**
     * Result object containing saved entities
     */
    public record PendingBookingResult(
            ResourceTimeBlock pendingBooking,
            WaitlistEntry waitlistEntry,
            Integer queuePosition
    ) {}

}
