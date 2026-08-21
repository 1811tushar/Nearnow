package com.nearnow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** What the client SENDS to POST /api/auth/forgot-password — just the
 * email they want to reset. Same @Email/@NotBlank pattern as every
 * other auth DTO in this package. */
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    public ForgotPasswordRequestDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
