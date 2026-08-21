package com.nearnow.auth;

import jakarta.validation.constraints.NotNull;

public class UpdateNotificationsRequestDTO {

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    public UpdateNotificationsRequestDTO() {
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}