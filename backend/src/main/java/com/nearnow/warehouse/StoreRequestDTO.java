package com.nearnow.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class StoreRequestDTO {
    @NotBlank private String name;
    @NotBlank private String addressLine;
    @NotBlank private String city;
    @NotBlank private String pincode;
    private double latitude;
    private double longitude;
    @PositiveOrZero private int capacity;
    private String operatingHoursStart;
    private String operatingHoursEnd;
    private Long warehouseManagerUserId;

    public StoreRequestDTO() {}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getAddressLine(){return addressLine;} public void setAddressLine(String v){addressLine=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public String getPincode(){return pincode;} public void setPincode(String v){pincode=v;}
    public double getLatitude(){return latitude;} public void setLatitude(double v){latitude=v;}
    public double getLongitude(){return longitude;} public void setLongitude(double v){longitude=v;}
    public int getCapacity(){return capacity;} public void setCapacity(int v){capacity=v;}
    public String getOperatingHoursStart(){return operatingHoursStart;} public void setOperatingHoursStart(String v){operatingHoursStart=v;}
    public String getOperatingHoursEnd(){return operatingHoursEnd;} public void setOperatingHoursEnd(String v){operatingHoursEnd=v;}
    public Long getWarehouseManagerUserId(){return warehouseManagerUserId;} public void setWarehouseManagerUserId(Long v){warehouseManagerUserId=v;}
}
