package com.nearnow.common.exception;

/**
 * Throw this when a request is well-formed (passes @Valid) but violates
 * a BUSINESS rule — e.g. placing an order with an empty cart, or (in a
 * future feature) reviewing a product never purchased. Different from
 * ResourceNotFoundException (something doesn't exist) and different
 * from validation errors (malformed input) — this is "the input is
 * fine, but the action doesn't make sense right now."
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
