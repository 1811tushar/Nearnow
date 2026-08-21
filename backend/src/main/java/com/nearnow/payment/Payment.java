package com.nearnow.payment;

import com.nearnow.auth.User;
import com.nearnow.order.Order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(String paymentReference, BigDecimal amount, User user) {
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.user = user;
        this.status = PaymentStatus.CREATED;
    }

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getPaymentReference() { return paymentReference; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public User getUser() { return user; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Instant getCreatedAt() { return createdAt; }
}
