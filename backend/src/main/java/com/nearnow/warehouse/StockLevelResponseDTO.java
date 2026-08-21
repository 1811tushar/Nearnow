package com.nearnow.warehouse;

public class StockLevelResponseDTO {

    private final Long id;
    private final Long storeId;
    private final Long productId;
    private final String productName;
    private final String barcode;
    private final int quantity;

    public StockLevelResponseDTO(Long id, Long storeId, Long productId, String productName,
                                 String barcode, int quantity) {
        this.id = id;
        this.storeId = storeId;
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getBarcode() { return barcode; }
    public int getQuantity() { return quantity; }
}
