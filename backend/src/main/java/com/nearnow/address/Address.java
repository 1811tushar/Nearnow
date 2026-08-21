package com.nearnow.address;

import com.nearnow.auth.User;
import jakarta.persistence.*;

/**
 * This is where User's savedAddresses field — deliberately excluded
 * back in Phase 2 — finally lands as its own normalized table, exactly
 * as flagged at the time.
 */
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String label;

    // Deliberately separate from User.fullName/phone — verified
    // AddressModel.dart carries its own fullName/phone, since an
    // address can be for ordering on someone else's behalf.
    private String fullName;
    private String phone;

    private String addressLine;
    private String city;
    private String pincode;

    private double latitude;
    private double longitude;

    @Column(nullable = false)
    private boolean isDefault;

    protected Address() {
    }

    public Address(User user, String label, String fullName, String phone, String addressLine,
                   String city, String pincode, double latitude, double longitude, boolean isDefault) {
        this.user = user;
        this.label = label;
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.city = city;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isDefault = isDefault;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}
