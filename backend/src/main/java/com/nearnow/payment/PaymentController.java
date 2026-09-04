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

    // Called directly by Razorpay's servers, not by our own frontend — no
    // JWT is (or can be) attached to this request, hence no `Authentication`
    // parameter. The @RequestBody is captured as a raw String deliberately:
    // signature verification needs the EXACT bytes Razorpay signed, and any
    // automatic JSON-to-DTO deserialization here would risk re-serializing
    // it slightly differently (field order, whitespace) and breaking the
    // signature check.
    @PostMapping("/webhook")
    public ResponseEntity<String> razorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String rawBody) {
        paymentService.handleRazorpayWebhook(rawBody, signature);
        return ResponseEntity.ok("ok");
    }
}
