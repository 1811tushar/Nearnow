package com.nearnow.common.dto;


import java.time.Instant;

/**
 * Universal response envelope — every controller returns this shape,
 * success or failure, so Flutter's fromJson logic stays identical
 * across every feature instead of guessing a different shape per endpoint.
 *
 * Success:  { "success": true,  "data": {...},        "message": null,          "timestamp": "..." }
 * Failure:  { "success": false, "data": null,          "message": "error text",  "timestamp": "..." }
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private Instant timestamp;

    // Private constructor — force callers through the static factory
    // methods below (success()/error()) so a response can never be built
    // in an inconsistent state (e.g. success=true with an error message).
    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    // Getters only — no setters. Once built via success()/error(), an
    // ApiResponse should never be mutated (this is a response DTO, not
    // a stateful object).
    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
