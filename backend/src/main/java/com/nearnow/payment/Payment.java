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

    // For MOCK mode this is "MOCK_<uuid>". For RAZORPAY mode this IS
    // Razorpay's own order id (e.g. "order_Abc123") — reusing this column
    // rather than adding a parallel "gatewayOrderId" column keeps the
    // existing findByPaymentReferenceForUpdate lookup working unchanged
    // for both modes.
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

    // Razorpay's own payment id (e.g. "pay_Xyz789"), set only once a
    // payment actually succeeds. Null for MOCK payments and for RAZORPAY
    // payments that were created but never completed. This is what a
    // refund or support-lookup call would key off later.
    @Column(nullable = true)
    private String gatewayPaymentId;

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
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }
    public Instant getCreatedAt() { return createdAt; }
}
