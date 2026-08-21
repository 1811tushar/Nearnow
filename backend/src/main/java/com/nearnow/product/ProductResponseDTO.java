package com.nearnow.product;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Notice effectivePrice and discountPercent are included here as plain
 * pre-computed values, not left for Flutter to derive itself. Same
 * "single source of truth" reasoning the Dart model's own comment
 * already stated for ProductModel — now the computation happens exactly
 * ONCE, on the backend, and every client (Flutter today, a future web
 * admin-dashboard tomorrow) gets the same already-correct number instead
 * of re-implementing the same ternary/rounding logic themselves.
 */
public class ProductResponseDTO implements Serializable { 

    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private List<String> images;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal effectivePrice;
    private int discountPercent;
    private String unit;
    private int stock;
    private double rating;
    private boolean isFeatured;
    private String barcode;
    private int reviewCount;
    private boolean active;

    @JsonCreator
    public ProductResponseDTO(@JsonProperty("id") Long id, @JsonProperty("name") String name,
                               @JsonProperty("description") String description, @JsonProperty("categoryId") Long categoryId,
                               @JsonProperty("images") List<String> images, @JsonProperty("price") BigDecimal price,
                               @JsonProperty("salePrice") BigDecimal salePrice, @JsonProperty("effectivePrice") BigDecimal effectivePrice,
                               @JsonProperty("discountPercent") int discountPercent, @JsonProperty("unit") String unit,
                               @JsonProperty("stock") int stock, @JsonProperty("rating") double rating,
                               @JsonProperty("isFeatured") boolean isFeatured, @JsonProperty("barcode") String barcode,
                               @JsonProperty("reviewCount") int reviewCount, @JsonProperty("active") boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.images = images;
        this.price = price;
        this.salePrice = salePrice;
        this.effectivePrice = effectivePrice;
        this.discountPercent = discountPercent;
        this.unit = unit;
        this.stock = stock;
        this.rating = rating;
        this.isFeatured = isFeatured;
        this.barcode = barcode;
        this.reviewCount = reviewCount;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public List<String> getImages() {
        return images;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public String getUnit() {
        return unit;
    }

    public int getStock() {
        return stock;
    }

    public double getRating() {
        return rating;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getReviewCount() { return reviewCount; }

    public boolean isActive() { return active; }
}