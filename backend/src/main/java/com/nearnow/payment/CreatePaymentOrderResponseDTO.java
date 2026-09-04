package com.nearnow.payment;

import java.math.BigDecimal;

public class CreatePaymentOrderResponseDTO {
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private String mode;
    // Razorpay's PUBLIC key id — safe to send to the client (it is not a
    // secret; only razorpay.key-secret on the backend is). The Flutter
    // app's Razorpay checkout widget needs this to open the payment sheet.
    // Null/omitted when mode is MOCK.
    private String razorpayKeyId;

    public CreatePaymentOrderResponseDTO(String paymentReference, BigDecimal amount, String currency, String mode) {
        this(paymentReference, amount, currency, mode, null);
    }

    public CreatePaymentOrderResponseDTO(String paymentReference, BigDecimal amount, String currency, String mode,
                                          String razorpayKeyId) {
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.currency = currency;
        this.mode = mode;
        this.razorpayKeyId = razorpayKeyId;
    }

    public String getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMode() { return mode; }
    public String getRazorpayKeyId() { return razorpayKeyId; }
}
