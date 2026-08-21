package com.nearnow.vendor;

import java.math.BigDecimal;

public class VendorProductResponseDTO {

    private final Long productId;
    private final String name;
    private final String barcode;
    private final BigDecimal price;
    private final BigDecimal salePrice;
    private final int stock;
    private final boolean warehouseManaged;
    private final boolean active;

    public VendorProductResponseDTO(Long productId, String name, String barcode,
                                    BigDecimal price, BigDecimal salePrice, int stock,
                                    boolean warehouseManaged, boolean active) {
        this.productId = productId;
        this.name = name;
        this.barcode = barcode;
        this.price = price;
        this.salePrice = salePrice;
        this.stock = stock;
        this.warehouseManaged = warehouseManaged;
        this.active = active;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public String getBarcode() { return barcode; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public int getStock() { return stock; }
    public boolean isWarehouseManaged() { return warehouseManaged; }
    public boolean isActive() { return active; }
}
