package com.nearnow.admin;

import jakarta.validation.constraints.NotBlank;

public class RoleAssignmentRequestDTO {

    @NotBlank(message = "Role is required")
    private String role;

    public RoleAssignmentRequestDTO() {
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
