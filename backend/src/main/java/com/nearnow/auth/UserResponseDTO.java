package com.nearnow.auth;


/**
 * What GET /api/auth/me returns — the full safe-to-expose profile shape,
 * once we already know who the caller is (via their JWT). No `token`
 * field here (unlike AuthResponseDTO) — this endpoint doesn't issue a
 * new token, it just answers "who am I logged in as."
 */
public class UserResponseDTO {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String photoUrl;
    private String role;
    private boolean isEmailVerified;
    private boolean notificationsEnabled;

    public UserResponseDTO(Long id, String email, String fullName, String phone,
                            String photoUrl, String role, boolean isEmailVerified,
                            boolean notificationsEnabled) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.role = role;
        this.isEmailVerified = isEmailVerified;
        this.notificationsEnabled = notificationsEnabled;
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

    public String getPhone() {
        return phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }
}
