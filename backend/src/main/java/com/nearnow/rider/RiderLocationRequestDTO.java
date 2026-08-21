package com.nearnow.rider;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class RiderLocationRequestDTO {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    @NotNull(message = "Availability is required")
    private Boolean available;

    public RiderLocationRequestDTO() {
    }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Boolean getAvailable() { return available; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setAvailable(Boolean available) { this.available = available; }
}
