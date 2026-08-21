package com.nearnow.cart;

import java.math.BigDecimal;
import java.util.List;

public class CartResponseDTO {
    private List<CartItemResponseDTO> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal grandTotal;

    public CartResponseDTO(List<CartItemResponseDTO> items, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal grandTotal) {
        this.items = items; this.subtotal = subtotal; this.deliveryFee = deliveryFee; this.grandTotal = grandTotal;
    }
    public List<CartItemResponseDTO> getItems() { return items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getGrandTotal() { return grandTotal; }
}
