package com.nearnow.payment;

import java.math.BigDecimal;

public class CreatePaymentOrderResponseDTO {
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private String mode;

    public CreatePaymentOrderResponseDTO(String paymentReference, BigDecimal amount, String currency, String mode) {
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.currency = currency;
        this.mode = mode;
    }

    public String getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMode() { return mode; }
}
