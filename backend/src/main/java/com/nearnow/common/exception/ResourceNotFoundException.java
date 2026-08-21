package com.nearnow.common.exception;

import com.nearnow.product.ProductService;

/**
 * Throw this from any Service when a requested entity doesn't exist —
 * e.g. ProductService.getById() when the id has no matching row.
 * GlobalExceptionHandler catches this and converts it to a 404 response
 * automatically, so every feature's Service just throws — no repeated
 * "if not found, build error response" code in every Controller.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
