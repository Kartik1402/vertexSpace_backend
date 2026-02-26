package com.example.vertexSpace.enums;

public enum WaitlistStatus {
    ACTIVE,      // Waiting in queue
    OFFERED,   // Got the slot (offer was accepted)
    CANCELLED,   // User left waitlist manually
    EXPIRED      // Slot time passed, no longer relevant
}
