package com.nearnow.common.exception;

import com.nearnow.common.dto.ApiResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * One place, for the whole backend, that decides "when X goes wrong,
 * here's the HTTP status + JSON shape Flutter receives." Every feature's
 * Controller/Service just throws the right exception type — this class
 * catches it centrally instead of every Controller writing its own
 * try/catch + error-JSON-building code.
 *
 * @RestControllerAdvice = "watch every @RestController in the app,
 * and if any of them lets one of these exception types escape, run the
 * matching @ExceptionHandler method below instead of returning Spring's
 * default HTML error page."
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Triggered by: any Service throwing `new ResourceNotFoundException(...)`
    // Example: OrderService.getOrderById() when the order id doesn't exist.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Triggered by: AuthService.register() when the email is already taken.
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Triggered by: AuthService.login() on any failure — see
    // InvalidCredentialsException's own comment for why both "email not
    // found" and "wrong password" land here with one generic message.
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Triggered by: business-rule violations — e.g. OrderService.placeOrder()
    // when the cart is empty. See InvalidOperationException's own comment.
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOperation(InvalidOperationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Triggered automatically by Spring itself whenever a @Valid-annotated
    // request DTO fails validation (e.g. @NotBlank name was blank,
    // @Email email was malformed). We don't throw this ourselves — Spring
    // throws it before the Controller method body even runs.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String combinedMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(combinedMessage));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The requested operation conflicts with existing data."));
    }

    // Catch-all safety net: anything NOT explicitly handled above still
    // comes back as clean JSON instead of a raw stack trace leaking to
    // the client. This is deliberately generic ("Something went wrong")
    // — the real exception detail goes to the server log, not the client,
    // since a raw exception message can leak internal implementation
    // details (table names, query structure) to whoever's calling the API.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
         ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(ApiResponse.error("Something went wrong. Please try again."));
    }
}
