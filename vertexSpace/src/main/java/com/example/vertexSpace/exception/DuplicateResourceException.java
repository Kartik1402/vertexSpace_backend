package com.example.vertexSpace.exception;
/**
 * Thrown when attempting to create a resource that already exists
 * (e.g., registering with an email that's already taken)
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
