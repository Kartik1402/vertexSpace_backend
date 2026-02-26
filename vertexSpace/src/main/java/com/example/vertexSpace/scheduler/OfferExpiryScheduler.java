package com.example.vertexSpace.scheduler;

import com.example.vertexSpace.service.OfferService;
import com.example.vertexSpace.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferExpiryScheduler {

    private final OfferService offerService;
    private final WaitlistService waitlistService;

    @Scheduled(fixedDelay = 60000)
    public void expireOffers() {
        try
        {
            log.debug("Running offer expiry check...");
            offerService.expireOffers();
        } catch (Exception e) {
            log.error("Error in offer expiry scheduler: {}", e.getMessage(), e);
        }
    }
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
