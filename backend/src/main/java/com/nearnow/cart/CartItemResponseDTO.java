package com.nearnow.cart;

import com.nearnow.product.Product;

import java.math.BigDecimal;

public class CartItemResponseDTO {

    private Long id;
    private Long productId;
    private String name;   // read live from Product join — see CartItem's class comment
    private String image;  // first image from Product.images, if any
    private BigDecimal price; // current server-authoritative effective price
    private String unit;
    private int quantity;
    private BigDecimal itemTotal;

    public CartItemResponseDTO(Long id, Long productId, String name, String image,
                                BigDecimal price, String unit, int quantity, BigDecimal itemTotal) {
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.price = price;
        this.unit = unit;
        this.quantity = quantity;
        this.itemTotal = itemTotal;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getUnit() {
        return unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getItemTotal() {
        return itemTotal;
    }
}
