package com.nearnow.warehouse;

import jakarta.validation.constraints.NotBlank;

public class PickItemRequestDTO {

    @NotBlank
    private String scannedBarcode;

    public PickItemRequestDTO() {
    }

    public String getScannedBarcode() { return scannedBarcode; }
    public void setScannedBarcode(String scannedBarcode) { this.scannedBarcode = scannedBarcode; }
}
