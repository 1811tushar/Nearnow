package com.nearnow.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequestDTO {

    @NotNull
    private Long productId;

    @Min(0)
    private int quantity;

    public StockAdjustmentRequestDTO() {
    }

    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }

    public void setProductId(Long productId) { this.productId = productId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
