package com.nearnow.vendor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class VendorProductUpdateRequestDTO {

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Sale price cannot be negative")
    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    public VendorProductUpdateRequestDTO() {
    }

    public BigDecimal getPrice() { return price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public int getStock() { return stock; }

    public void setPrice(BigDecimal price) { this.price = price; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public void setStock(int stock) { this.stock = stock; }
}
