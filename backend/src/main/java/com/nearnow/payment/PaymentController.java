package com.nearnow.payment;

import com.nearnow.common.dto.ApiResponse;
import com.nearnow.order.OrderResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreatePaymentOrderResponseDTO>> createOrder(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.createPaymentOrder(authentication.getName())));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> verify(Authentication authentication,
                                                                 @Valid @RequestBody VerifyPaymentRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.verifyPayment(authentication.getName(), request), "Payment confirmed, order placed"));
    }
}
