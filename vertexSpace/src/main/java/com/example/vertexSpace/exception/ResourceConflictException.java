package com.example.vertexSpace.exception;

import com.example.vertexSpace.dto.response.BookingConflictResponseDTO;
import com.example.vertexSpace.dto.response.BookingWithWaitlistResponse;
import lombok.Getter;

/**
 * Exception thrown when resource booking conflicts with existing reservation
 */
@Getter
public class ResourceConflictException extends RuntimeException {

    private final BookingConflictResponseDTO conflictDetails;
    private final BookingWithWaitlistResponse waitlistResponse;

    // Constructor for old flow (without auto-waitlist)
    public ResourceConflictException(String message, BookingConflictResponseDTO conflictDetails) {
        super(message);
        this.conflictDetails = conflictDetails;
        this.waitlistResponse = null;
    }

    // Constructor for new flow (with auto-waitlist)
    public ResourceConflictException(String message, BookingWithWaitlistResponse waitlistResponse) {
        super(message);
        this.conflictDetails = null;
        this.waitlistResponse = waitlistResponse;
    }

    public ResourceConflictException(String message) {
        super(message);
        this.conflictDetails = null;
        this.waitlistResponse = null;
    }
}
