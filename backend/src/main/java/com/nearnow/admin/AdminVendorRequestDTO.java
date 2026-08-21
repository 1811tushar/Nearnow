package com.nearnow.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminVendorRequestDTO {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business address is required")
    private String businessAddress;

    private String gstNumber;

    public AdminVendorRequestDTO() {
    }

    public Long getUserId() { return userId; }
    public String getBusinessName() { return businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public String getGstNumber() { return gstNumber; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
}
