package com.example.vertexSpace.enums;

/**
 * Types of resources that can be booked
 */
public enum ResourceType {
    /**
     * Meeting rooms, conference rooms
     * Requires capacity field
     */
    ROOM,

    /**
     * Individual workstations
     * Capacity not required (always 1)
     */
    DESK,

    /**
     * Parking spots
     * Capacity not required (always 1)
     */
    PARKING
}

