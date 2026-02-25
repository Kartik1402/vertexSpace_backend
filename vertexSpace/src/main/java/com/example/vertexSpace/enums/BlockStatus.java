package com.example.vertexSpace.enums;

/**
 * Status of time blocks
 *
 * For BOOKING type:
 * - CONFIRMED: Active booking
 * - CANCELLED: User cancelled
 *
 * For OFFER_HOLD type:
 * - OFFERED: Waiting for user response
 * - ACCEPTED: User accepted (transitional, booking gets created)
 * - DECLINED: User declined
 * - EXPIRED: 10-minute window passed
 */
public enum BlockStatus {
    // Booking statuses
    CONFIRMED,    // Booking is active
    CANCELLED,
    PENDING_WAITLIST,// Booking was cancelled

    // Offer-specific statuses
    OFFERED,      // Offer sent, awaiting response
    ACCEPTED,     // User accepted offer (creates new BOOKING)
    DECLINED,     // User declined offer
    EXPIRED       // Offer expired (10-minute timeout)
}
