package com.nearnow.auth;

import com.nearnow.common.exception.GlobalExceptionHandler;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What Flutter SENDS to POST /api/auth/register.
 *
 * @NotBlank / @Email / @Size are validation annotations — Spring checks
 * these automatically the moment this DTO is bound from the incoming
 * JSON, BEFORE your Controller method body even runs (see
 * GlobalExceptionHandler.handleValidation() from Phase 1 — that's what
 * catches a failure here and turns it into a 400 response).
 */
public class RegisterRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    public RegisterRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
