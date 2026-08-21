package com.nearnow.rider;

public class RiderResponseDTO {

    private final Long id;
    private final Long userId;
    private final String email;
    private final String vehicleType;
    private final String vehicleNumber;
    private final double currentLatitude;
    private final double currentLongitude;
    private final boolean active;
    private final boolean available;

    public RiderResponseDTO(Long id, Long userId, String email, String vehicleType,
                            String vehicleNumber, double currentLatitude, double currentLongitude,
                            boolean active, boolean available) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.active = active;
        this.available = available;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public boolean isActive() { return active; }
    public boolean isAvailable() { return available; }
}
