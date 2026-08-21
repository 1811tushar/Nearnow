package com.nearnow.order;

import java.math.BigDecimal;

public class OrderItemResponseDTO {
    private Long productId;
    private String name;
    private String image;
    private BigDecimal price;
    private String unit;
    private int quantity;
    private BigDecimal itemTotal;

    public OrderItemResponseDTO(Long productId, String name, String image, BigDecimal price,
                                 String unit, int quantity, BigDecimal itemTotal) {
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.price = price;
        this.unit = unit;
        this.quantity = quantity;
        this.itemTotal = itemTotal;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public String getImage() { return image; }
    public BigDecimal getPrice() { return price; }
    public String getUnit() { return unit; }
    public int getQuantity() { return quantity; }
    public BigDecimal getItemTotal() { return itemTotal; }
}
