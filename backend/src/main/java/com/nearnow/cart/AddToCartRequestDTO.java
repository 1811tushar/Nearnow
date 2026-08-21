package com.nearnow.cart;

import com.nearnow.product.Product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddToCartRequestDTO {

    @NotNull(message = "Product id is required")
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    public AddToCartRequestDTO() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
