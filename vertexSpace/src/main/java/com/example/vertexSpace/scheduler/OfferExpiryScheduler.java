package com.example.vertexSpace.scheduler;

import com.example.vertexSpace.service.OfferService;
import com.example.vertexSpace.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Offer Expiry Scheduler
 *
 * Runs every 1 minute to:
 * 1. Expire offers past their 10-minute window
 * 2. Expire old waitlist entries (past time slots)
 *
 * Uses @Scheduled with fixedDelay to ensure tasks don't overlap
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfferExpiryScheduler {

    private final OfferService offerService;
    private final WaitlistService waitlistService;

    /**
     * Expire offers every 1 minute
     *
     * Finds offers where:
     * - block_type = 'OFFER_HOLD'
     * - status = 'OFFERED'
     * - expires_at_utc <= NOW()
     *
     * Changes status to 'EXPIRED' and triggers next person in queue
     */
    @Scheduled(fixedDelay = 60000) // 60 seconds = 1 minute
    public void expireOffers() {
        try {
            log.debug("Running offer expiry check...");
            offerService.expireOffers();
        } catch (Exception e) {
            log.error("Error in offer expiry scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Expire old waitlist entries every 5 minutes
     *
     * Entries where start_utc < NOW() and status = 'ACTIVE'
     * are marked as 'EXPIRED' (slot time has passed)
     */
    @Scheduled(fixedDelay = 300000) // 300 seconds = 5 minutes
    public void expireOldWaitlistEntries() {
        try {
            log.debug("Running waitlist entry expiry check...");
            waitlistService.expireOldEntries();
        } catch (Exception e) {
            log.error("Error in waitlist expiry scheduler: {}", e.getMessage(), e);
        }
    }
}
