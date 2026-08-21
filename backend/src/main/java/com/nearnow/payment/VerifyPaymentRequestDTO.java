package com.nearnow.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VerifyPaymentRequestDTO {
    @NotBlank private String paymentReference;
    @NotBlank private String outcome;
    @NotNull(message = "Delivery address is required") private Long addressId;

    public VerifyPaymentRequestDTO() {}
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
}
