package com.example.vertexSpace.exception;

/**
 * Exception thrown when user tries to accept an expired offer
 *
 * HTTP 410 Gone (resource existed but is no longer available)
 */
public class OfferExpiredException extends RuntimeException {

    public OfferExpiredException(String message) {
        super(message);
    }

    public OfferExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
