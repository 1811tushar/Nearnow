package com.nearnow.vendor;

import com.nearnow.auth.User;
import jakarta.persistence.*;

/**
 * Merchant/business profile attached to an existing authenticated User.
 * Authentication remains in auth.User; this entity only stores business
 * ownership data needed by the vendor domain.
 */
@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessAddress;

    private String gstNumber;

    @Column(nullable = false)
    private boolean active = true;

    protected Vendor() {
    }

    public Vendor(User user, String businessName, String businessAddress, String gstNumber) {
        this.user = user;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.gstNumber = gstNumber;
        this.active = true;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getBusinessName() { return businessName; }
    public String getBusinessAddress() { return businessAddress; }
    public String getGstNumber() { return gstNumber; }
    public boolean isActive() { return active; }

    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
    public void setActive(boolean active) { this.active = active; }
}
