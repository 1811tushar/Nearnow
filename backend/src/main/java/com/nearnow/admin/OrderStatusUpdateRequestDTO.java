package com.nearnow.admin;

import com.nearnow.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderStatusUpdateRequestDTO {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    public OrderStatusUpdateRequestDTO() {}

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
