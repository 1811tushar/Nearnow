package com.nearnow.vendor;

import jakarta.validation.constraints.NotBlank;

public class VendorProfileRequestDTO {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business address is required")
    private String businessAddress;

    private String gstNumber;

    public VendorProfileRequestDTO() {
    }

    public String getBusinessName() { return businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public String getGstNumber() { return gstNumber; }

    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
}
