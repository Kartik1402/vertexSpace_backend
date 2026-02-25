package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.waitlist.AcceptOfferResponseDTO;
import com.example.vertexSpace.dto.waitlist.OfferResponseDTO;
import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.ResourceTimeBlock;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.entity.WaitlistEntry;
import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.BlockType;
import com.example.vertexSpace.enums.WaitlistStatus;
import com.example.vertexSpace.exception.AuthorizationException;
import com.example.vertexSpace.exception.ConflictException;
import com.example.vertexSpace.exception.InvalidStateException;
import com.example.vertexSpace.exception.OfferExpiredException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.ResourceTimeBlockRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.vertexSpace.service.impl.NotificationServiceImpl;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final ResourceTimeBlockRepository blockRepo;
    private final WaitlistEntryRepository waitlistRepo;
    private final UserRepository userRepo;
    private final NotificationServiceImpl notificationService;

    private static final int OFFER_EXPIRY_MINUTES = 10;

    private static final DateTimeFormatter IST_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a z").withZone(ZoneId.of("Asia/Kolkata"));

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Create offer for waitlist entry (called by WaitlistService.processNextInWaitlist())
     */
    @Transactional
    public void createOffer(WaitlistEntry entry) {
        log.info("Creating offer for waitlist entry {}", entry.getId());

        Resource resource = entry.getResource();
        User user = entry.getUser();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(OFFER_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        ResourceTimeBlock offer = new ResourceTimeBlock();
        offer.setBlockType(BlockType.OFFER_HOLD);
        offer.setStatus(BlockStatus.OFFERED);
        offer.setResource(resource);
        offer.setUser(user);
        offer.setStartTimeUtc(entry.getStartUtc());
        offer.setEndTimeUtc(entry.getEndUtc());
        offer.setWaitlistEntryId(entry.getId());
        offer.setExpiresAtUtc(expiresAt);

        blockRepo.save(offer);

        log.info("Offer created: {} for user {} (expires at {})",
                offer.getId(), user.getEmail(), expiresAt
        );
    }

    /**
     * GET /api/v1/me/waitlist-offers
     */
    @Transactional(readOnly = true)
    public List<OfferResponseDTO> getMyActiveOffers(UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        List<ResourceTimeBlock> offers = blockRepo.findActiveOffers(
                currentUser.getId(),
                Instant.now()
        );

        return offers.stream()
                .map(this::mapToOfferDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/v1/waitlist-offers/{offerId}
     */
    @Transactional(readOnly = true)
    public OfferResponseDTO getOfferDetails(UUID offerId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        ResourceTimeBlock offer = blockRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getUser().getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Not your offer");
        }

        if (offer.getBlockType() != BlockType.OFFER_HOLD) {
            throw new IllegalArgumentException("Not an offer");
        }

        return mapToOfferDTO(offer);
    }

    /**
     * POST /api/v1/waitlist-offers/{offerId}/accept
     */
//            @Transactional
//    public AcceptOfferResponseDTO acceptOffer(UUID offerId, UUID currentUserId) {
//        User currentUser = requireUser(currentUserId);
//
//        log.info("User {} accepting offer {}", currentUser.getEmail(), offerId);
//
//        ResourceTimeBlock offer = blockRepo.findByIdForUpdate(offerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));
//
//        Resource resource = offer.getResource();
//
//        if (!offer.getUser().getId().equals(currentUser.getId())) {
//            throw new AuthorizationException("Not your offer");
//        }
//
//        if (offer.getBlockType() != BlockType.OFFER_HOLD) {
//            throw new IllegalArgumentException("Not an offer");
//        }
//
//        if (offer.getStatus() != BlockStatus.OFFERED) {
//            throw new InvalidStateException(
//                    "Offer already processed. Current status: " + offer.getStatus()
//            );
//        }
//
//        if (offer.getExpiresAtUtc().isBefore(Instant.now())) {
//            String expiredTime = IST_FORMATTER.format(offer.getExpiresAtUtc());
//            throw new OfferExpiredException("This offer expired at " + expiredTime);
//        }
//
//        Instant bufferEnd = offer.getEndTimeUtc().plus(15, ChronoUnit.MINUTES);
//
//        boolean hasConflict = blockRepo.existsActiveBlockExcluding(
//                resource.getId(),
//                offer.getStartTimeUtc(),
//                bufferEnd,
//                offerId
//        );
//
//        if (hasConflict) {
//            throw new ConflictException("Slot no longer available due to conflict");
//        }
//
//
//        // Mark offer ACCEPTED
//        offer.setStatus(BlockStatus.ACCEPTED);
//        offer.setRespondedAt(Instant.now());
//        blockRepo.save(offer);
//
//        // Create booking
//        ResourceTimeBlock booking = new ResourceTimeBlock();
//        booking.setBlockType(BlockType.BOOKING);
//        booking.setStatus(BlockStatus.CONFIRMED);
//        booking.setResource(resource);
//        booking.setUser(currentUser);
//        booking.setStartTimeUtc(offer.getStartTimeUtc());
//        booking.setEndTimeUtc(offer.getEndTimeUtc());
//        booking.setWaitlistEntryId(offer.getWaitlistEntryId());
//
//        ResourceTimeBlock savedBooking = blockRepo.save(booking);
//
//        // Update waitlist entry
//        if (offer.getWaitlistEntryId() != null) {
//            waitlistRepo.updateStatus(
//                    offer.getWaitlistEntryId(),
//                    WaitlistStatus.OFFERED,
//                    Instant.now()
//            );
//        }
//        String timeSlot = formatTimeSlot(offer.getStartTimeUtc(), offer.getEndTimeUtc());
//        notificationService.notifyBookingConfirmed(
//                        currentUserId,
//                savedBooking.getId(),
//                offer.getResource().getName(),
//                timeSlot
//        );
//        return AcceptOfferResponseDTO.builder()
//                .bookingId(savedBooking.getId())
//                .status(savedBooking.getStatus().name())
//                .startUtc(savedBooking.getStartTimeUtc())
//                .endUtc(savedBooking.getEndTimeUtc())
//                .resourceName(resource.getName())
//                .message("Offer accepted! Booking confirmed.")
//                .build();
//    }
    @Transactional
    public AcceptOfferResponseDTO acceptOffer(UUID offerId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        ResourceTimeBlock block = blockRepo.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!block.getUser().getId().equals(currentUser.getId())) throw new AuthorizationException("Not your offer");
        if (block.getBlockType() != BlockType.OFFER_HOLD) throw new IllegalArgumentException("Not an offer");
        if (block.getStatus() != BlockStatus.OFFERED)
            throw new InvalidStateException("Offer already processed. Current status: " + block.getStatus());
        if (block.getExpiresAtUtc().isBefore(Instant.now()))
            throw new OfferExpiredException("This offer expired at " + IST_FORMATTER.format(block.getExpiresAtUtc()));

        Instant bufferEnd = block.getEndTimeUtc().plus(15, ChronoUnit.MINUTES);

        boolean hasConflict = blockRepo.existsActiveBlockExcluding(
                block.getResource().getId(),
                block.getStartTimeUtc(),
                bufferEnd,
                offerId
        );
        if (hasConflict) throw new ConflictException("Slot no longer available due to conflict");

        // Convert OFFER_HOLD -> BOOKING (no new insert, so no overlap)
        block.setBlockType(BlockType.BOOKING);
        block.setStatus(BlockStatus.CONFIRMED);
        block.setRespondedAt(Instant.now());
        block.setConflictEndUtc(bufferEnd);          // important if your constraint uses conflict_end_utc
        // block.setBufferMinutes(15);               // if you track it
        // block.setPurpose(...);                    // if needed
        // block.setExpiresAtUtc(null);              // optional: offer is no longer expirable

        ResourceTimeBlock savedBooking = blockRepo.save(block);

        if (savedBooking.getWaitlistEntryId() != null) {
            waitlistRepo.updateStatus(savedBooking.getWaitlistEntryId(), WaitlistStatus.OFFERED, Instant.now());
            // (Your current code sets OFFERED here—likely wrong on acceptance.)
        }

        String timeSlot = formatTimeSlot(savedBooking.getStartTimeUtc(), savedBooking.getEndTimeUtc());
        notificationService.notifyBookingConfirmed(
                currentUserId,
                savedBooking.getId(),
                savedBooking.getResource().getName(),
                timeSlot
        );

        return AcceptOfferResponseDTO.builder()
                .bookingId(savedBooking.getId())
                .status(savedBooking.getStatus().name())
                .startUtc(savedBooking.getStartTimeUtc())
                .endUtc(savedBooking.getEndTimeUtc())
                .resourceName(savedBooking.getResource().getName())
                .message("Offer accepted! Booking confirmed.")
                .build();
    }

    /**
     * POST /api/v1/waitlist-offers/{offerId}/decline
     */
    @Transactional
    public void declineOffer(UUID offerId, UUID currentUserId) {
        User currentUser = requireUser(currentUserId);

        log.info("User {} declining offer {}", currentUser.getEmail(), offerId);

        ResourceTimeBlock offer = blockRepo.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getUser().getId().equals(currentUser.getId())) {
            throw new AuthorizationException("Not your offer");
        }

        if (offer.getStatus() != BlockStatus.OFFERED) {
            throw new InvalidStateException("Offer already processed");
        }

        offer.setStatus(BlockStatus.DECLINED);
        offer.setRespondedAt(Instant.now());
        blockRepo.save(offer);

        processNextAfterDecline(offer);
    }

    @Transactional
    public void expireOffers() {
        List<ResourceTimeBlock> expired = blockRepo.findExpiredOffers(Instant.now());
        if (expired.isEmpty()) return;

        for (ResourceTimeBlock offer : expired) {
            try {
                processExpiredOffer(offer);
            } catch (Exception e) {
                log.error("Error processing expired offer {}: {}", offer.getId(), e.getMessage(), e);
            }
        }
    }

    private void processExpiredOffer(ResourceTimeBlock offer) {
        offer.setStatus(BlockStatus.EXPIRED);
        blockRepo.save(offer);
        processNextAfterExpiry(offer);
    }

    @Transactional
    public void cancelOfferByWaitlistEntry(UUID waitlistEntryId) {
        var offerOpt = blockRepo.findByWaitlistEntryIdAndBlockType(
                waitlistEntryId,
                BlockType.OFFER_HOLD
        );

        if (offerOpt.isEmpty()) return;

        ResourceTimeBlock offer = offerOpt.get();
        if (offer.getStatus() == BlockStatus.OFFERED) {
            offer.setStatus(BlockStatus.CANCELLED);
            blockRepo.save(offer);
            processNextAfterDecline(offer);
        }
    }

    private void processNextAfterDecline(ResourceTimeBlock offer) {
        log.info("Offer declined/cancelled - next person should be notified");
        // TODO: publish event / call WaitlistService via events
    }

    private void processNextAfterExpiry(ResourceTimeBlock offer) {
        log.info("Offer expired - triggering next in queue");
        // TODO: publish event / call WaitlistService via events
    }
    @Transactional
    public void processWaitlistForCancelledBooking(UUID resourceId, Instant startTime, Instant endTime) {
        log.info("Processing waitlist for cancelled booking: resource={}, time={} to {}",
                resourceId, startTime, endTime);

        // Find next person in queue for this resource and time slot
        List<WaitlistEntry> eligibleEntries = waitlistRepo.findActiveEntriesForResourceAndTimeRange(
                resourceId,
                startTime,
                endTime
        );

        if (eligibleEntries.isEmpty()) {
            log.info("No one in waitlist for this time slot");
            return;
        }

        // Get first in queue (should be ordered by queue position)
        WaitlistEntry nextInQueue = eligibleEntries.get(0);

        log.info("Found next person in queue: user={}, position={}",
                nextInQueue.getUser().getEmail(),
                nextInQueue.getQueuePosition());

        // Update waitlist entry status
        nextInQueue.setStatus(WaitlistStatus.OFFERED);
        nextInQueue.setOfferedAt(Instant.now());
        nextInQueue.setOfferExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        waitlistRepo.save(nextInQueue);

        // Create the offer using your existing method
        createOffer(nextInQueue);

        log.info("Offer sent to user {} for cancelled booking slot", nextInQueue.getUser().getEmail());

        String timeSlot = formatTimeSlot(startTime, endTime);
        notificationService.notifyWaitlistOffer(
                nextInQueue.getUser().getId(),
                nextInQueue.getId(),
                nextInQueue.getResource().getName(),
                timeSlot,
                10 // minutes remaining
        );
    }
        private String formatTimeSlot(Instant start, Instant end) {
            // Format: "Jan 15, 2PM-3PM"
            return start.toString() + " - " + end.toString(); // Improve formatting as needed
        }


    private OfferResponseDTO mapToOfferDTO(ResourceTimeBlock offer) {
        long remainingSeconds = ChronoUnit.SECONDS.between(Instant.now(), offer.getExpiresAtUtc());
        if (remainingSeconds < 0) remainingSeconds = 0;

        return OfferResponseDTO.builder()
                .id(offer.getId())
                .resourceId(offer.getResource().getId())
                .resourceName(offer.getResource().getName())
                .resourceType(offer.getResource().getResourceType().name())
                .startUtc(offer.getStartTimeUtc())
                .endUtc(offer.getEndTimeUtc())
                .status(offer.getStatus().name())
                .offeredAt(offer.getCreatedAtUtc())
                .expiresAt(offer.getExpiresAtUtc())
                .remainingSeconds(remainingSeconds)
                .waitlistEntryId(offer.getWaitlistEntryId())
                .build();
    }
}
