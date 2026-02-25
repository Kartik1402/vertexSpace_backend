package com.example.vertexSpace.enums;

/**
 * Status of waitlist entries
 *
 * ACTIVE: Currently waiting in queue
 * FULFILLED: Received and accepted offer (success!)
 * CANCELLED: User left waitlist
 * EXPIRED: Time slot passed while still waiting
 */
public enum WaitlistStatus {
    ACTIVE,      // Waiting in queue
    OFFERED,   // Got the slot (offer was accepted)
    CANCELLED,   // User left waitlist manually
    EXPIRED      // Slot time passed, no longer relevant
}
