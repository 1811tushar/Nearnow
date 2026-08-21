package com.nearnow.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/**
 * Didn't exist until now — Phase 4 scoped Product to Response-only DTOs
 * since no create-product UI existed at the time. Admin is what
 * actually needs this.
 */
public class ProductRequestDTO {

    @NotBlank private String name;
    private String description;
    @NotNull private Long categoryId;
    private List<String> images;

    @NotNull @PositiveOrZero
    private BigDecimal price;

    private BigDecimal salePrice = BigDecimal.ZERO;
    private String unit;

    @PositiveOrZero
    private int stock;

    private boolean isFeatured;

    public ProductRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }
}
