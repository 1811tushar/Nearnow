package com.nearnow.rider;

import com.nearnow.auth.User;
import jakarta.persistence.*;

@Entity
@Table(name = "riders", indexes = {
        @Index(name = "idx_rider_available", columnList = "active, available")
})
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false, unique = true)
    private String vehicleNumber;

    private double currentLatitude;
    private double currentLongitude;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean available = true;

    protected Rider() {
    }

    public Rider(User user, String vehicleType, String vehicleNumber) {
        this.user = user;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.active = true;
        this.available = true;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public boolean isActive() { return active; }
    public boolean isAvailable() { return available; }

    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public void setCurrentLatitude(double currentLatitude) { this.currentLatitude = currentLatitude; }
    public void setCurrentLongitude(double currentLongitude) { this.currentLongitude = currentLongitude; }
    public void setActive(boolean active) { this.active = active; }
    public void setAvailable(boolean available) { this.available = available; }
}
