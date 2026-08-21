package com.nearnow.vendor;

public class VendorResponseDTO {

    private final Long id;
    private final Long userId;
    private final String email;
    private final String businessName;
    private final String businessAddress;
    private final String gstNumber;
    private final boolean active;

    public VendorResponseDTO(Long id, Long userId, String email, String businessName,
                             String businessAddress, String gstNumber, boolean active) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.gstNumber = gstNumber;
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getBusinessName() { return businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public String getGstNumber() { return gstNumber; }
    public boolean isActive() { return active; }
}
