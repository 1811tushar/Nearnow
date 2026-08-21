package com.nearnow.cart;


import jakarta.validation.constraints.Min;

public class UpdateQuantityRequestDTO {

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public UpdateQuantityRequestDTO() {
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
