package com.nearnow.order;

import com.nearnow.auth.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // cascade = ALL: saving/deleting an Order automatically saves/deletes
    // its OrderItems too — we never manage OrderItem rows independently.
    // orphanRemoval = true: if an item is ever removed from this list in
    // code, Hibernate deletes that row (not applicable in practice here
    // since orders are immutable after creation, but it's the correct
    // default for an owned collection like this).
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Inventory bookkeeping is explicit so cancellation can restore stock
    // exactly once. `inventoryReserved` also makes the schema safe for
    // legacy orders created before stock reservation was introduced:
    // those rows default to false and must not magically add stock back.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean inventoryReserved = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean stockRestored = false;

    private String paymentMethod;

    @Embedded
    private DeliveryAddressSnapshot deliveryAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {
    }

    public Order(User user, BigDecimal totalAmount, String paymentMethod, DeliveryAddressSnapshot deliveryAddress) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PLACED;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public boolean isInventoryReserved() { return inventoryReserved; }
    public void setInventoryReserved(boolean inventoryReserved) { this.inventoryReserved = inventoryReserved; }
    public boolean isStockRestored() { return stockRestored; }
    public void setStockRestored(boolean stockRestored) { this.stockRestored = stockRestored; }
    public String getPaymentMethod() { return paymentMethod; }
    public DeliveryAddressSnapshot getDeliveryAddress() { return deliveryAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
