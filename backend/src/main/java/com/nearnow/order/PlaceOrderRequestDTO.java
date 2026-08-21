package com.nearnow.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Deliberately narrow — NO items, NO totalAmount. This is the fix for
 * the client-trusted-pricing gap found in the old order_provider.dart
 * (placeOrder() there accepted items+totalAmount straight from the
 * caller). The backend now derives both from the server's own Cart
 * state — the client has no way to influence price or line-items at all.
 */
public class PlaceOrderRequestDTO {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    public PlaceOrderRequestDTO() {}

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
