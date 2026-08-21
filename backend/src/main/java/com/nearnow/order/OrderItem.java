package com.nearnow.order;

import com.nearnow.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Nullable, deliberately — kept as an FK for analytics/reorder
    // linkage, but if the Product is ever deleted, this order's history
    // must NOT break. The denormalized fields below (name/image/price/
    // unit) are what actually render this line item; `product` is
    // secondary reference data, not load-bearing for display.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Denormalized snapshot, frozen at order-time — a receipt must show
    // exactly what was ordered, even if the product's name/image/price
    // changes afterward. More aggressive denormalization than CartItem
    // (which only snapshots price) because Order is a permanent record,
    // Cart is a live, temporary state.
    @Column(nullable = false)
    private String name;
    private String image;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String unit;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
    }

    public OrderItem(Product product, String name, String image, BigDecimal price, String unit, int quantity) {
        this.product = product;
        this.name = name;
        this.image = image;
        this.price = price;
        this.unit = unit;
        this.quantity = quantity;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getItemTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Product getProduct() { return product; }
    public String getName() { return name; }
    public String getImage() { return image; }
    public BigDecimal getPrice() { return price; }
    public String getUnit() { return unit; }
    public int getQuantity() { return quantity; }
}
