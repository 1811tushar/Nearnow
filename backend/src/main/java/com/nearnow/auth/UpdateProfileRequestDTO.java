package com.nearnow.auth;

import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String photoUrl;

    public UpdateProfileRequestDTO() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}