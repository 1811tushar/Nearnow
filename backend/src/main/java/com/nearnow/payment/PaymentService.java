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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Zero-cost payment gateway used for local development and demos. */
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final String paymentMode;
    private final PricingService pricingService;

    public PaymentService(PaymentRepository paymentRepository, CartRepository cartRepository,
                          CartItemRepository cartItemRepository, UserRepository userRepository,
                          ProductRepository productRepository, OrderService orderService,
                          OrderRepository orderRepository, PricingService pricingService,
                          @Value("${payment.mode:MOCK}") String paymentMode) {
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.paymentMode = paymentMode;
    }

    @Transactional
    public CreatePaymentOrderResponseDTO createPaymentOrder(String userEmail) {
        if (!"MOCK".equalsIgnoreCase(paymentMode)) {
            throw new InvalidOperationException("Only MOCK payment mode is enabled in this free development build");
        }
        User user = getUser(userEmail);
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cart is empty"));
        List<CartItem> items = repriceCart(cart);
        if (items.isEmpty()) throw new InvalidOperationException("Cannot pay for an empty cart");
        BigDecimal amount = pricingService.total(total(items));

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

    @Transactional
    public OrderResponseDTO verifyPayment(String userEmail, VerifyPaymentRequestDTO request) {
        User user = getUser(userEmail);
        Payment payment = paymentRepository.findByPaymentReferenceForUpdate(request.getPaymentReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUser().getId().equals(user.getId())) throw new ResourceNotFoundException("Payment not found");
        if (payment.getStatus() != PaymentStatus.CREATED) throw new InvalidOperationException("This payment has already been processed");

        if (!"SUCCESS".equalsIgnoreCase(request.getOutcome())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new InvalidOperationException("Mock payment was marked as failed");
        }

        Cart lockedCart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cart is empty"));
        List<CartItem> currentItems = repriceCart(lockedCart);
        BigDecimal currentAmount = pricingService.total(total(currentItems));
        if (currentAmount.compareTo(payment.getAmount()) != 0) {
            throw new InvalidOperationException("Cart total changed. Please restart checkout.");
        }

        PlaceOrderRequestDTO place = new PlaceOrderRequestDTO();
        place.setAddressId(request.getAddressId());
        place.setPaymentMethod("Mock Online Payment");
        OrderResponseDTO order = orderService.placeOrder(userEmail, place);
        payment.setStatus(PaymentStatus.PAID);
        payment.setOrder(orderRepository.findById(order.getId()).orElseThrow());
        paymentRepository.save(payment);
        return order;
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
