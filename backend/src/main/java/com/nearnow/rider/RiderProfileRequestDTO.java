package com.nearnow.rider;

import jakarta.validation.constraints.NotBlank;

public class RiderProfileRequestDTO {

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    public RiderProfileRequestDTO() {
    }

    public String getVehicleType() { return vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }

    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
}
