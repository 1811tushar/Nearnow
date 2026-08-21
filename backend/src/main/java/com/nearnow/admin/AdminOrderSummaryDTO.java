package com.nearnow.admin;

import com.nearnow.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Deliberately lighter than order.OrderResponseDTO (which includes the
 * full item list + delivery-address snapshot) — an admin scanning ALL
 * orders needs a scannable summary row, not every order's full detail
 * inline. Full detail is still just a GET /api/orders/{id} away if an
 * admin needs to drill into one (reusing the existing customer-facing
 * endpoint — admins are still users, just with an elevated role).
 */
public class AdminOrderSummaryDTO {

    private Long id;
    private String userEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;

    public AdminOrderSummaryDTO(Long id, String userEmail, BigDecimal totalAmount,
                                 OrderStatus status, Instant createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
