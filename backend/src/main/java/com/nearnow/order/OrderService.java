package com.nearnow.order;

import com.nearnow.address.Address;
import com.nearnow.address.AddressRepository;
import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.cart.Cart;
import com.nearnow.cart.CartItem;
import com.nearnow.cart.CartItemRepository;
import com.nearnow.cart.CartRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.notification.NotificationService;
import com.nearnow.payment.PaymentRepository;
import com.nearnow.payment.PaymentStatus;
import com.nearnow.common.pricing.PricingService;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.warehouse.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProductRepository productRepository;
    private final WarehouseService warehouseService;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                         CartItemRepository cartItemRepository, AddressRepository addressRepository,
                         UserRepository userRepository, NotificationService notificationService,
                         ProductRepository productRepository, WarehouseService warehouseService,
                         PaymentRepository paymentRepository, PricingService pricingService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.productRepository = productRepository;
        this.warehouseService = warehouseService;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
    }

    // @Transactional matters a lot here: order-creation + cart-clearing
    // must happen as one atomic unit. If the order saved but the cart
    // failed to clear (or vice versa), the user would see duplicate
    // items on their next order — the transaction boundary prevents
    // that half-completed state.
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public OrderResponseDTO placeOrder(String userEmail, PlaceOrderRequestDTO request) {
        User user = getUser(userEmail);

        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new InvalidOperationException("Cannot place an order with an empty cart");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + request.getAddressId()));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found: " + request.getAddressId());
        }

        DeliveryAddressSnapshot snapshot = new DeliveryAddressSnapshot(
                address.getLabel(), address.getFullName(), address.getPhone(), address.getAddressLine(),
                address.getCity(), address.getPincode(), address.getLatitude(), address.getLongitude()
        );

        // Checkout is the server-side price boundary. Re-read and lock every
        // product, then refresh the cart snapshot to the current effective price.
        // The client never supplies an amount.
        for (CartItem cartItem : cartItems.stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).toList()) {
            Product current = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));
            if (!current.isActive()) throw new InvalidOperationException("Product is no longer available: " + current.getName());
            if (cartItem.getQuantity() > current.getStock()) {
                throw new InvalidOperationException("Only " + current.getStock() + " unit(s) available for " + current.getName());
            }
            cartItem.setPriceAtAdd(current.getEffectivePrice());
        }
        cartItemRepository.saveAll(cartItems);
        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = pricingService.total(subtotal);

        Order order = new Order(user, totalAmount, request.getPaymentMethod(), snapshot);

        // Product data is read before the order is persisted so the receipt
        // snapshot remains stable even if the product changes later.
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProduct().getId()));
            if (!product.isActive()) {
                throw new InvalidOperationException("Product is no longer available: " + product.getName());
            }

            OrderItem orderItem = new OrderItem(
                    product,
                    product.getName(),
                    product.getImages().isEmpty() ? null : product.getImages().get(0),
                    cartItem.getPriceAtAdd(),
                    product.getUnit(),
                    cartItem.getQuantity()
            );
            order.addItem(orderItem);
        }

        Order saved = orderRepository.save(order);

        /*
         * Warehouse-managed products use Store/StockLevel as the inventory
         * source of truth. Legacy products that have not yet been migrated
         * keep the existing Product.stock path so introducing the warehouse
         * package does not silently break old seeded/demo products.
         */
        boolean warehouseManaged = warehouseService.reserveForOrder(saved);
        if (!warehouseManaged) {
            reserveLegacyStock(saved);
            saved.setInventoryReserved(true);
            orderRepository.save(saved);
        }

        // Cart cleared as part of the same transaction as order-creation.
        cartItemRepository.deleteByCartId(cart.getId());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.sendOrderConfirmation(saved);
            }
        });

        return toDTO(saved);
    }

    private void reserveLegacyStock(Order order) {
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        Map<Long, Product> lockedProducts = new HashMap<>();
        for (OrderItem item : items) {
            Long productId = item.getProduct().getId();
            lockedProducts.put(productId, productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId)));
        }

        for (OrderItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (item.getQuantity() > product.getStock()) {
                throw new InvalidOperationException(
                        "Only " + product.getStock() + " unit(s) available for " + product.getName());
            }
        }

        for (OrderItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }

    public List<OrderResponseDTO> getOrderHistory(String userEmail) {
        User user = getUser(userEmail);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDTO).toList();
    }

    public OrderResponseDTO getOrderById(String userEmail, Long orderId) {
        return toDTO(getOwnedOrder(userEmail, orderId));
    }

    // This is the exact logic that used to live in Firestore Security
    // Rules (per the old OrderService.dart comment) — now explicit,
    // testable Java code instead of a separate rules-DSL.
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public OrderResponseDTO cancelOrder(String userEmail, Long orderId) {
        Order order = getOwnedOrder(userEmail, orderId);

        boolean cancellable = order.getStatus() == OrderStatus.PLACED
                || order.getStatus() == OrderStatus.PACKED;
        if (!cancellable) {
            throw new InvalidOperationException(
                    "Order cannot be cancelled once it is " + order.getStatus());
        }

        if (order.isInventoryReserved() && !order.isStockRestored()) {
            boolean warehouseManaged = warehouseService.restoreReservedStock(order);
            if (!warehouseManaged) {
                restoreStock(order);
            }
            order.setStockRestored(true);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });
        return toDTO(saved);
    }

    private void restoreStock(Order order) {
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        for (OrderItem item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + item.getProduct().getId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private Order getOwnedOrder(String userEmail, Long orderId) {
        User user = getUser(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private OrderResponseDTO toDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream().map(item ->
                new OrderItemResponseDTO(
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getName(), item.getImage(), item.getPrice(), item.getUnit(),
                        item.getQuantity(), item.getItemTotal()
                )
        ).toList();

        boolean cancellable = order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PACKED;

        return new OrderResponseDTO(order.getId(), itemDTOs, order.getTotalAmount(), order.getStatus(),
                cancellable, order.getPaymentMethod(), order.getDeliveryAddress(), order.getCreatedAt());
    }
}
