package com.example.vertexSpace.exception;

/**
 * Exception thrown when an operation is attempted on an entity in invalid state
 *
 * Examples:
 * - Accepting an already-processed offer
 * - Cancelling a cancelled booking
 * - Modifying a fulfilled waitlist entry
 *
 * Maps to HTTP 400 Bad Request
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
