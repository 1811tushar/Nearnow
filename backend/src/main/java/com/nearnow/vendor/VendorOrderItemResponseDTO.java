package com.nearnow.vendor;

import java.math.BigDecimal;

public class VendorOrderItemResponseDTO {

    private final Long productId;
    private final String name;
    private final BigDecimal price;
    private final int quantity;
    private final BigDecimal itemTotal;

    public VendorOrderItemResponseDTO(Long productId, String name, BigDecimal price,
                                      int quantity, BigDecimal itemTotal) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.itemTotal = itemTotal;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public BigDecimal getItemTotal() { return itemTotal; }
}
