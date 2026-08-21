package com.nearnow.order;

import com.nearnow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(
            Authentication authentication, @Valid @RequestBody PlaceOrderRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.placeOrder(authentication.getName(), request), "Order placed"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrderHistory(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderHistory(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(
            Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(authentication.getName(), id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(authentication.getName(), id), "Order cancelled"));
    }
}
