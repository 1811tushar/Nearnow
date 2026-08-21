package com.nearnow.auth;

import jakarta.validation.constraints.NotBlank;

/** What the client SENDS to POST /api/auth/refresh — a still-valid
 * refresh token, in exchange for a new short-lived access token. */
public class RefreshRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshRequestDTO() {
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
