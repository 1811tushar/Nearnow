package com.nearnow.product;

import com.nearnow.category.Category;
import com.nearnow.vendor.Vendor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Java-mirror of ProductModel (product_model.dart), with one deliberate
 * type change flagged in the Approach Comparison table: price/salePrice
 * are BigDecimal here, not double.
 *
 * WHY: `double` stores money as a binary floating-point approximation —
 * 19.99 isn't exactly representable in binary, the same way 1/3 isn't
 * exactly representable in decimal. Add enough of these approximate
 * values together (e.g. summing an Order's line items) and the errors
 * compound into real, visible rounding mistakes — a cart total that's
 * off by a paisa. BigDecimal stores the exact decimal value, no
 * approximation, which is why it's the industry-standard type for
 * currency in Java, full stop.
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_barcode", columnList = "barcode"),
        @Index(name = "idx_product_name", columnList = "name")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Nullable for backward compatibility: products created before the
     * Vendor phase remain valid and simply have no vendor owner. Once set,
     * VendorService uses this relationship as the ownership boundary for
     * vendor product operations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    // A List<String> can't be a normal @Column — it's not a single
    // scalar value. @ElementCollection tells Hibernate "build a
    // separate side-table (product_images) just to hold this list,
    // linked back to this product's id" — this is how a relational DB
    // represents "one row has many of this simple value", the equivalent
    // of Firestore's images array field.
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    private String unit;

    @Column(nullable = false)
    private int stock;

    private double rating;

    @Column(nullable = false)
    private boolean isFeatured;

    @Column(unique = true)
    private String barcode;

    // Soft-delete flag. Products remain referenced by historical orders,
    // cart items, wishlists and reviews, so deleting the row would violate
    // foreign keys or destroy historical meaning. Customer-facing queries
    // filter this flag instead.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // Denormalized, same as the Dart model's own comment explains —
    // kept in sync by ReviewService whenever a review is added (Phase 9),
    // not computed live from a COUNT query on every product read.
    @Column(nullable = false)
    private int reviewCount = 0;

    protected Product() {
    }

    public Product(String name, String description, Category category, BigDecimal price, String unit, int stock) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.stock = stock;
    }

    // Java-mirror of ProductModel's effectivePrice getter — same logic,
    // same reason (single source of truth instead of duplicating this
    // ternary in every Controller/DTO that needs it).
    public BigDecimal getEffectivePrice() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 ? salePrice : price;
    }

    // Java-mirror of ProductModel's discountPercent getter.
    public int getDiscountPercent() {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0
                || salePrice.compareTo(price) >= 0
                || price.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal diff = price.subtract(salePrice);
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(price, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public void setFeatured(boolean featured) {
        isFeatured = featured;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }
}
