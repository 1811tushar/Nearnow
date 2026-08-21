package com.nearnow.rider;

import jakarta.validation.constraints.NotNull;

public class RiderAssignmentStatusRequestDTO {

    @NotNull(message = "Status is required")
    private DeliveryAssignmentStatus status;

    public RiderAssignmentStatusRequestDTO() {
    }

    public DeliveryAssignmentStatus getStatus() { return status; }
    public void setStatus(DeliveryAssignmentStatus status) { this.status = status; }
}
