package com.nearnow.address;

public class AddressResponseDTO {

    private Long id;
    private String label;
    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String pincode;
    private double latitude;
    private double longitude;
    private boolean isDefault;

    public AddressResponseDTO(Long id, String label, String fullName, String phone, String addressLine,
                               String city, String pincode, double latitude, double longitude, boolean isDefault) {
        this.id = id;
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
    public String getLabel() { return label; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isDefault() { return isDefault; }
}
