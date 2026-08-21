package com.nearnow.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponseDTO {
    private Long id;
    private List<OrderItemResponseDTO> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private boolean isCancellable;
    private String paymentMethod;
    private DeliveryAddressSnapshot deliveryAddress;
    private Instant createdAt;

    public OrderResponseDTO(Long id, List<OrderItemResponseDTO> items, BigDecimal totalAmount,
                             OrderStatus status, boolean isCancellable, String paymentMethod,
                             DeliveryAddressSnapshot deliveryAddress, Instant createdAt) {
        this.id = id;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.isCancellable = isCancellable;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public List<OrderItemResponseDTO> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public boolean isCancellable() { return isCancellable; }
    public String getPaymentMethod() { return paymentMethod; }
    public DeliveryAddressSnapshot getDeliveryAddress() { return deliveryAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
