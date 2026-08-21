package com.nearnow.auth;


import jakarta.validation.constraints.NotBlank;

/** What Flutter SENDS to POST /api/auth/login. */
public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequestDTO() {
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
}
