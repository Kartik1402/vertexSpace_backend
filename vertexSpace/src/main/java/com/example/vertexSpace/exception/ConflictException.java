package com.example.vertexSpace.exception;

/**
 * Exception thrown when a resource conflict occurs
 *
 * Common scenarios:
 * - Time slot already booked
 * - User already in waitlist for slot
 * - Concurrent booking attempts
 * - Offer acceptance conflict
 *
 * Maps to HTTP 409 Conflict
 */
public class ConflictException extends RuntimeException {

    /**
     * Constructor with message only
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
