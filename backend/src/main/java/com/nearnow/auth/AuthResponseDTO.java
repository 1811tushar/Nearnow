package com.nearnow.auth;


/**
 * What Flutter RECEIVES back from both register and login — the JWT
 * token plus enough basic user-info to populate the app's UserProvider
 * immediately, without a second round-trip.
 *
 * Notice `passwordHash` is NOT a field here — this is the DTO rule from
 * Section 8, Checkpoint 3 in practice: the Entity's most sensitive field
 * physically cannot leak to the client, because this class never had it
 * to begin with.
 */
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private Long id;
    private String email;
    private String fullName;
    private String role;

    public AuthResponseDTO(String token, String refreshToken, Long id, String email, String fullName, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}
