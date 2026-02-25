package com.example.vertexSpace.exception;
/**
 * Thrown when user doesn't have permission for requested operation
 * (e.g., Department Admin trying to manage another department's resources)
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}

