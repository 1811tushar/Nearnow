package com.nearnow.rider;

import com.nearnow.order.Order;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "delivery_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delivery_assignment_order", columnNames = "order_id")
})
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryAssignmentStatus status = DeliveryAssignmentStatus.ASSIGNED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutAmount;

    @Column(nullable = false)
    private double distanceKm;

    protected DeliveryAssignment() {
    }

    public DeliveryAssignment(Order order, Rider rider, BigDecimal payoutAmount, double distanceKm) {
        this.order = order;
        this.rider = rider;
        this.payoutAmount = payoutAmount;
        this.distanceKm = distanceKm;
        this.status = DeliveryAssignmentStatus.ASSIGNED;
    }

    @PrePersist
    protected void onCreate() {
        this.assignedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Rider getRider() { return rider; }
    public Instant getAssignedAt() { return assignedAt; }
    public DeliveryAssignmentStatus getStatus() { return status; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public double getDistanceKm() { return distanceKm; }

    public void setStatus(DeliveryAssignmentStatus status) { this.status = status; }
}
