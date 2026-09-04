package com.nearnow.payment;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.cart.Cart;
import com.nearnow.cart.CartItem;
import com.nearnow.cart.CartItemRepository;
import com.nearnow.cart.CartRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.OrderResponseDTO;
import com.nearnow.order.OrderRepository;
import com.nearnow.order.OrderService;
import com.nearnow.order.PlaceOrderRequestDTO;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.common.pricing.PricingService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Two payment modes live side by side here, selected by payment.mode:
 *
 *  - MOCK: zero-cost, no external calls — used for local dev/demos and by
 *    the whole existing test suite. Unchanged from before this file grew
 *    a second mode.
 *
 *  - RAZORPAY: real orders created against Razorpay's TEST environment
 *    (https://dashboard.razorpay.com, Test Mode toggle — no real money
 *    moves, but the flow, UPI-app redirect, and signatures are all real).
 *    Two independent signature checks exist for it:
 *      1. Client-side, in verifyPayment() below — the HMAC formula
 *         Razorpay's own docs specify, proving the success callback the
 *         Flutter app received wasn't forged by someone just POSTing a
 *         fake "SUCCESS" straight to this API.
 *      2. Webhook-side, in handleRazorpayWebhook() — a second, independent
 *         signature over the raw webhook body, which exists specifically
 *         to catch the case where the user's connection drops right after
 *         paying and the app-side callback never arrives. Razorpay still
 *         calls the webhook in that case; without it, a real successful
 *         payment could be lost with no order ever created for it.
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final String paymentMode;
    private final PricingService pricingService;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;

    public PaymentService(PaymentRepository paymentRepository, CartRepository cartRepository,
                          CartItemRepository cartItemRepository, UserRepository userRepository,
                          ProductRepository productRepository, OrderService orderService,
                          OrderRepository orderRepository, PricingService pricingService,
                          @Value("${payment.mode:MOCK}") String paymentMode,
                          @Value("${razorpay.key-id:}") String razorpayKeyId,
                          @Value("${razorpay.key-secret:}") String razorpayKeySecret,
                          @Value("${razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.paymentMode = paymentMode;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    @Transactional
    public CreatePaymentOrderResponseDTO createPaymentOrder(String userEmail) {
        User user = getUser(userEmail);
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cart is empty"));
        List<CartItem> items = repriceCart(cart);
        if (items.isEmpty()) throw new InvalidOperationException("Cannot pay for an empty cart");
        BigDecimal amount = pricingService.total(total(items));

        if ("RAZORPAY".equalsIgnoreCase(paymentMode)) {
            return createRazorpayOrder(user, amount);
        }
        if (!"MOCK".equalsIgnoreCase(paymentMode)) {
            throw new InvalidOperationException("Unsupported payment mode configured: " + paymentMode);
        }

        // One active checkout per user: retrying the same button does not create
        // an unbounded number of payment intents.
        return paymentRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), PaymentStatus.CREATED)
                .filter(existing -> existing.getAmount().compareTo(amount) == 0)
                .map(existing -> new CreatePaymentOrderResponseDTO(existing.getPaymentReference(), amount, "INR", "MOCK"))
                .orElseGet(() -> {
                    Payment payment = new Payment("MOCK_" + UUID.randomUUID(), amount, user);
                    paymentRepository.save(payment);
                    return new CreatePaymentOrderResponseDTO(payment.getPaymentReference(), amount, "INR", "MOCK");
                });
    }

    private CreatePaymentOrderResponseDTO createRazorpayOrder(User user, BigDecimal amount) {
        if (razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) {
            throw new InvalidOperationException(
                    "Razorpay is not configured on this server (missing RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET)");
        }
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            // Razorpay's amount unit is paise, not rupees — 100 paise = ₹1.
            // intValueExact() deliberately throws rather than silently
            // truncating if a future currency/precision change ever made
            // this multiplication produce a non-integer paise amount.
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValueExact());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "nearnow_" + user.getId() + "_" + System.currentTimeMillis());

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = new Payment(razorpayOrderId, amount, user);
            paymentRepository.save(payment);

            return new CreatePaymentOrderResponseDTO(razorpayOrderId, amount, "INR", "RAZORPAY", razorpayKeyId);
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for user {}: {}", user.getId(), e.getMessage());
            throw new InvalidOperationException("Could not start payment. Please try again.");
        }
    }

    @Transactional
    public OrderResponseDTO verifyPayment(String userEmail, VerifyPaymentRequestDTO request) {
        boolean isRazorpayCallback = request.getRazorpayPaymentId() != null
                && request.getRazorpayOrderId() != null
                && request.getRazorpaySignature() != null;

        if (isRazorpayCallback) {
            return verifyRazorpayPayment(getUser(userEmail), request);
        }
        return verifyMockPayment(getUser(userEmail), request);
    }

    private OrderResponseDTO verifyMockPayment(User user, VerifyPaymentRequestDTO request) {
        Payment payment = paymentRepository.findByPaymentReferenceForUpdate(request.getPaymentReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUser().getId().equals(user.getId())) throw new ResourceNotFoundException("Payment not found");
        if (payment.getStatus() != PaymentStatus.CREATED) throw new InvalidOperationException("This payment has already been processed");

        if (!"SUCCESS".equalsIgnoreCase(request.getOutcome())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new InvalidOperationException("Mock payment was marked as failed");
        }

        OrderResponseDTO order = repriceAndPlaceOrder(user, payment, request.getAddressId(), "Mock Online Payment");
        payment.setStatus(PaymentStatus.PAID);
        payment.setOrder(orderRepository.findById(order.getId()).orElseThrow());
        paymentRepository.save(payment);
        return order;
    }

    private OrderResponseDTO verifyRazorpayPayment(User user, VerifyPaymentRequestDTO request) {
        Payment payment = paymentRepository.findByPaymentReferenceForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUser().getId().equals(user.getId())) throw new ResourceNotFoundException("Payment not found");
        if (payment.getStatus() != PaymentStatus.CREATED) throw new InvalidOperationException("This payment has already been processed");

        // Exact formula from Razorpay's own docs: HMAC-SHA256 of
        // "<order_id>|<payment_id>" using the account's key secret. This
        // is the only thing that actually proves the success callback the
        // app received came from Razorpay and wasn't just a client lying.
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String expectedSignature = hmacSha256Hex(payload, razorpayKeySecret);
        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new InvalidOperationException("Payment verification failed");
        }

        OrderResponseDTO order = repriceAndPlaceOrder(user, payment, request.getAddressId(), "Razorpay");
        payment.setStatus(PaymentStatus.PAID);
        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        payment.setOrder(orderRepository.findById(order.getId()).orElseThrow());
        paymentRepository.save(payment);
        return order;
    }

    /**
     * Razorpay's server-to-server webhook — configured in the Razorpay
     * dashboard to call POST /api/payments/webhook, independent of
     * whether the paying user's app ever calls verifyPayment() at all.
     *
     * This exists as a reconciliation safety net, not the primary
     * order-creation path: if the connection drops after a successful
     * UPI/card payment but before the Flutter app's success callback
     * fires, this webhook is Razorpay's only way to tell us the payment
     * actually succeeded. It marks the Payment row PAID so the money
     * isn't silently "lost" from our records — but it deliberately does
     * NOT create the Order itself, because order creation needs the
     * user's cart + chosen delivery address, neither of which Razorpay's
     * webhook payload carries. Building order-creation into the webhook
     * path would need a redesign (e.g. snapshotting the cart/address at
     * order-creation time) that is out of scope here; this is a
     * documented boundary, not an oversight.
     */
    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signature) {
        if (razorpayWebhookSecret.isBlank()) {
            log.warn("Received a Razorpay webhook but RAZORPAY_WEBHOOK_SECRET is not configured — ignoring.");
            return;
        }
        String expectedSignature = hmacSha256Hex(rawBody, razorpayWebhookSecret);
        if (!expectedSignature.equals(signature)) {
            throw new InvalidOperationException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(rawBody);
        String eventType = event.optString("event", "");

        JSONObject paymentEntity = extractPaymentEntity(event);
        if (paymentEntity == null) return; // not a payment event we care about

        String razorpayOrderId = paymentEntity.optString("order_id", null);
        String razorpayPaymentId = paymentEntity.optString("id", null);
        if (razorpayOrderId == null) return;

        paymentRepository.findByPaymentReferenceForUpdate(razorpayOrderId).ifPresent(payment -> {
            // Razorpay retries webhooks on any non-2xx response, and can
            // occasionally deliver the same event twice even on success.
            // Re-processing an already-settled payment must be a safe
            // no-op — never a double status flip, never a duplicate order.
            if (payment.getStatus() != PaymentStatus.CREATED) return;

            if ("payment.captured".equals(eventType)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setGatewayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);
            } else if ("payment.failed".equals(eventType)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
    }

    private JSONObject extractPaymentEntity(JSONObject event) {
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) return null;
        JSONObject paymentWrapper = payload.optJSONObject("payment");
        if (paymentWrapper == null) return null;
        return paymentWrapper.optJSONObject("entity");
    }

    private static String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    @Transactional
    public void markRefundedForOrder(Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });
    }

    private OrderResponseDTO repriceAndPlaceOrder(User user, Payment payment, Long addressId, String paymentMethodLabel) {
        Cart lockedCart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cart is empty"));
        List<CartItem> currentItems = repriceCart(lockedCart);
        BigDecimal currentAmount = pricingService.total(total(currentItems));
        if (currentAmount.compareTo(payment.getAmount()) != 0) {
            throw new InvalidOperationException("Cart total changed. Please restart checkout.");
        }

        PlaceOrderRequestDTO place = new PlaceOrderRequestDTO();
        place.setAddressId(addressId);
        place.setPaymentMethod(paymentMethodLabel);
        return orderService.placeOrder(user.getEmail(), place);
    }

    private List<CartItem> repriceCart(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        for (CartItem item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getProduct().getId()));
            if (!product.isActive()) throw new InvalidOperationException("Product is no longer available: " + product.getName());
            if (item.getQuantity() > product.getStock()) throw new InvalidOperationException("Insufficient stock for " + product.getName());
            if (item.getPriceAtAdd().compareTo(product.getEffectivePrice()) != 0) item.setPriceAtAdd(product.getEffectivePrice());
        }
        cartItemRepository.saveAll(items);
        return items;
    }

    private BigDecimal total(List<CartItem> items) {
        return items.stream().map(CartItem::getItemTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
