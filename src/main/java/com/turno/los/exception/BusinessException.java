package com.turno.los.exception;

/**
 * Thrown when a business rule is violated (e.g. agent tries to decide on a
 * loan that is not in UNDER_REVIEW, or no agents are available).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
