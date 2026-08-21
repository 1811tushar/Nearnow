package com.nearnow.warehouse;

public class StoreResponseDTO {
    private Long id; private String name; private String addressLine; private String city; private String pincode;
    private double latitude; private double longitude; private int capacity; private String operatingHoursStart; private String operatingHoursEnd;
    private boolean active; private Long warehouseManagerUserId; private String warehouseManagerEmail;
    public StoreResponseDTO(Long id,String name,String addressLine,String city,String pincode,double latitude,double longitude,int capacity,String start,String end,boolean active,Long managerId,String managerEmail){
        this.id=id;this.name=name;this.addressLine=addressLine;this.city=city;this.pincode=pincode;this.latitude=latitude;this.longitude=longitude;this.capacity=capacity;this.operatingHoursStart=start;this.operatingHoursEnd=end;this.active=active;this.warehouseManagerUserId=managerId;this.warehouseManagerEmail=managerEmail;
    }
    public Long getId(){return id;} public String getName(){return name;} public String getAddressLine(){return addressLine;} public String getCity(){return city;} public String getPincode(){return pincode;}
    public double getLatitude(){return latitude;} public double getLongitude(){return longitude;} public int getCapacity(){return capacity;} public String getOperatingHoursStart(){return operatingHoursStart;} public String getOperatingHoursEnd(){return operatingHoursEnd;} public boolean isActive(){return active;}
    public Long getWarehouseManagerUserId(){return warehouseManagerUserId;} public String getWarehouseManagerEmail(){return warehouseManagerEmail;}
}
