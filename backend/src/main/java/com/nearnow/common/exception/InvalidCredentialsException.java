package com.nearnow.common.exception;


/**
 * Throw this for ANY login failure — email not found, OR email found but
 * password wrong. Both cases throw this SAME exception with the SAME
 * generic message ("Invalid email or password").
 *
 * Why not two different exceptions/messages: if "email not found" and
 * "wrong password" gave different responses, an attacker could use that
 * difference to figure out which emails are registered on NearNow at
 * all (an information-leak, same family of problem as
 * GlobalExceptionHandler's generic 500 message from Phase 1).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
