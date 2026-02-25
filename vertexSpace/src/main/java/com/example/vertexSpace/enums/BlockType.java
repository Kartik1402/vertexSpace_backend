package com.example.vertexSpace.enums;

/**
 * Type of time block in unified resource_time_blocks table
 */
public enum BlockType {
    /**
     * Regular booking created by user
     */
    BOOKING,

    /**
     * Temporary hold for waitlist offer
     * User has 15 minutes to accept
     */
    OFFER_HOLD
}
