package com.nearnow.warehouse;

import com.nearnow.auth.User;
import jakarta.persistence.*;

import java.time.LocalTime;

/**
 * A physical NearNow dark-store/warehouse.
 *
 * The warehouse manager relationship is intentionally owned by Store rather
 * than User: one manager is assigned to one store, while User keeps the
 * existing authentication model unchanged.
 */
@Entity
@Table(name = "stores", indexes = {
        @Index(name = "idx_store_active", columnList = "active")
})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String addressLine;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int capacity;

    private LocalTime operatingHoursStart;
    private LocalTime operatingHoursEnd;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Ownership boundary for ROLE_WAREHOUSE_MANAGER.
     * A manager is assigned to exactly one store in this phase.
     * Role assignment itself remains an admin/direct-DB operation.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_manager_user_id", unique = true)
    private User warehouseManager;

    protected Store() {
    }

    public Store(String name, String addressLine, String city, String pincode,
                 double latitude, double longitude, int capacity,
                 LocalTime operatingHoursStart, LocalTime operatingHoursEnd) {
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.operatingHoursStart = operatingHoursStart;
        this.operatingHoursEnd = operatingHoursEnd;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public LocalTime getOperatingHoursStart() { return operatingHoursStart; }
    public LocalTime getOperatingHoursEnd() { return operatingHoursEnd; }
    public boolean isActive() { return active; }
    public User getWarehouseManager() { return warehouseManager; }

    public void setName(String name) { this.name = name; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setOperatingHoursStart(LocalTime operatingHoursStart) { this.operatingHoursStart = operatingHoursStart; }
    public void setOperatingHoursEnd(LocalTime operatingHoursEnd) { this.operatingHoursEnd = operatingHoursEnd; }
    public void setActive(boolean active) { this.active = active; }
    public void setWarehouseManager(User warehouseManager) { this.warehouseManager = warehouseManager; }
}
