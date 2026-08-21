package com.nearnow.warehouse;

public class PickListItemResponseDTO {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final String barcode;
    private final int quantity;
    private final boolean picked;

    public PickListItemResponseDTO(Long id, Long productId, String productName,
                                   String barcode, int quantity, boolean picked) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.barcode = barcode;
        this.quantity = quantity;
        this.picked = picked;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getBarcode() { return barcode; }
    public int getQuantity() { return quantity; }
    public boolean isPicked() { return picked; }
}
