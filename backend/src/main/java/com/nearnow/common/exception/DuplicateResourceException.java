package com.nearnow.common.exception;

import com.nearnow.auth.AuthService;

/**
 * Throw this when trying to create something that already exists —
 * e.g. AuthService.register() when the email is already taken.
 * Semantically different from ResourceNotFoundException (that's "doesn't
 * exist", this is "already exists") — different HTTP status too (409
 * Conflict, not 404).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
