package com.nearnow.rider;

import java.math.BigDecimal;
import java.time.Instant;

public class DeliveryAssignmentResponseDTO {

    private final Long id;
    private final Long orderId;
    private final Long riderId;
    private final DeliveryAssignmentStatus status;
    private final Instant assignedAt;
    private final BigDecimal payoutAmount;
    private final double distanceKm;

    public DeliveryAssignmentResponseDTO(Long id, Long orderId, Long riderId,
                                         DeliveryAssignmentStatus status, Instant assignedAt,
                                         BigDecimal payoutAmount, double distanceKm) {
        this.id = id;
        this.orderId = orderId;
        this.riderId = riderId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.payoutAmount = payoutAmount;
        this.distanceKm = distanceKm;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getRiderId() { return riderId; }
    public DeliveryAssignmentStatus getStatus() { return status; }
    public Instant getAssignedAt() { return assignedAt; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public double getDistanceKm() { return distanceKm; }
}
