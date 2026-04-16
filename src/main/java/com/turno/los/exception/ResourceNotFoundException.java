package com.turno.los.exception;

/**
 * Thrown when a requested resource (loan, agent) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
