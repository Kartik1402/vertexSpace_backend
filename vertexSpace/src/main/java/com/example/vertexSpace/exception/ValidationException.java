package com.example.vertexSpace.exception;
/**
 * Thrown when business rule validation fails
 * (e.g., booking outside allowed time, invalid grid alignment)
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
