package com.nearnow.vendor;

import com.nearnow.order.OrderStatus;

import java.time.Instant;
import java.util.List;

public class VendorOrderResponseDTO {

    private final Long orderId;
    private final OrderStatus status;
    private final Instant createdAt;
    private final List<VendorOrderItemResponseDTO> items;

    public VendorOrderResponseDTO(Long orderId, OrderStatus status, Instant createdAt,
                                  List<VendorOrderItemResponseDTO> items) {
        this.orderId = orderId;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<VendorOrderItemResponseDTO> getItems() { return items; }
}
