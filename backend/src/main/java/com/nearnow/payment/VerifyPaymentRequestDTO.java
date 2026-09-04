package com.nearnow.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VerifyPaymentRequestDTO {
    @NotBlank private String paymentReference;

    // Only required for MOCK mode ("SUCCESS"/"FAILED" simulated outcome).
    // Left optional (no @NotBlank) here because a real Razorpay
    // verification instead relies on razorpaySignature below — enforcing
    // "outcome required" at the DTO level would break the Razorpay path.
    // PaymentService itself decides which fields it actually needs based
    // on which ones are present.
    private String outcome;

    @NotNull(message = "Delivery address is required") private Long addressId;

    // Present only when verifying a real Razorpay payment. These three
    // together are exactly what Razorpay's Checkout success callback hands
    // back to the client, and exactly what PaymentService needs to
    // recompute and check the HMAC signature server-side.
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;

    public VerifyPaymentRequestDTO() {}
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
}
