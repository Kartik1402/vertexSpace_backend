package com.example.vertexSpace.enums;

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
