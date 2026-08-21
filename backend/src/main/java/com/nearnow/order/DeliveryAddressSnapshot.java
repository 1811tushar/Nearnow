package com.nearnow.order;

import jakarta.persistence.Embeddable;

/**
 * @Embeddable = a value-object whose fields get inlined as columns
 * directly into Order's own table (no separate delivery_address_snapshot
 * table, no foreign key) — unlike @ManyToOne, there's no shared row here,
 * because this is a FROZEN COPY, not a live reference. Deliberately NOT
 * a reference to Address — if the source Address is later edited or
 * deleted, this snapshot must stay exactly as it was at order-time.
 */
@Embeddable
public class DeliveryAddressSnapshot {

    private String label;
    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String pincode;
    private double latitude;
    private double longitude;

    protected DeliveryAddressSnapshot() {
    }

    public DeliveryAddressSnapshot(String label, String fullName, String phone, String addressLine,
                                    String city, String pincode, double latitude, double longitude) {
        this.label = label;
        this.fullName = fullName;
        this.phone = phone;
        this.addressLine = addressLine;
        this.city = city;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLabel() { return label; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
