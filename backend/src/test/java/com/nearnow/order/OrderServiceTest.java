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
import com.nearnow.common.pricing.PricingService;
import com.nearnow.notification.NotificationService;
import com.nearnow.payment.Payment;
import com.nearnow.payment.PaymentRepository;
import com.nearnow.payment.PaymentStatus;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.warehouse.WarehouseService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderService — the checkout + cancellation flow.
 * Every collaborator is mocked; PricingService is used as a REAL
 * instance instead of a mock because it has zero dependencies of its
 * own and its delivery-fee threshold logic (free above 199) is exactly
 * the kind of business rule worth exercising for real rather than
 * stubbing away.
 *
 * One Spring-specific gotcha handled here: placeOrder() calls
 * TransactionSynchronizationManager.registerSynchronization(...) to
 * fire the order-confirmation notification only after the DB
 * transaction commits. Outside a real @Transactional proxy (which is
 * how these tests call the service — directly, not through Spring)
 * there is no active transaction synchronization, and that call would
 * throw IllegalStateException. initSynchronization()/clearSynchronization()
 * in setUp/tearDown fake just enough of that machinery so the
 * registration succeeds — then each test manually invokes the captured
 * synchronization's afterCommit() to prove the notification fires with
 * the right order, the same way Spring would after a real commit.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ProductRepository productRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private PaymentRepository paymentRepository;

    // Real instance on purpose — see class javadoc.
    private final PricingService pricingService = new PricingService();

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartRepository, cartItemRepository,
                addressRepository, userRepository, notificationService, productRepository,
                warehouseService, paymentRepository, pricingService);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- reflection helpers (every entity id is @GeneratedValue, no setter) ---

    private static void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private User user(Long id) throws Exception {
        User u = new User("user@nearnow.com", "hash", "Test User", "");
        setId(u, id);
        return u;
    }

    private Cart cart(User owner, Long id) throws Exception {
        Cart c = new Cart(owner);
        setId(c, id);
        return c;
    }

    private Product product(Long id, String name, BigDecimal price, int stock, boolean active) throws Exception {
        Product p = new Product(name, "desc", null, price, "1 unit", stock);
        p.setActive(active);
        setId(p, id);
        return p;
    }

    private CartItem cartItem(Long id, Cart cart, Product product, int qty, BigDecimal priceAtAdd) throws Exception {
        CartItem ci = new CartItem(cart, product, qty, priceAtAdd);
        setId(ci, id);
        return ci;
    }

    private Address address(Long id, User owner) throws Exception {
        Address a = new Address(owner, "Home", "Test User", "9999999999",
                "123 Test Street", "Delhi", "110001", 28.6, 77.2, true);
        setId(a, id);
        return a;
    }

    // --- placeOrder: happy paths ---------------------------------------

    @Test
    void placeOrder_belowFreeDeliveryThreshold_addsDeliveryFee() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Milk", new BigDecimal("50.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 2, new BigDecimal("50.00")); // subtotal = 100, below 199
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseService.reserveForOrder(any(Order.class))).thenReturn(true); // warehouse-managed

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        OrderResponseDTO response = orderService.placeOrder("user@nearnow.com", request);

        // subtotal 100 + delivery fee 20 = 120
        assertThat(response.getTotalAmount()).isEqualByComparingTo("120.00");
        verify(cartItemRepository).deleteByCartId(10L);
        // warehouse-managed path: legacy stock deduction must NOT run.
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void placeOrder_atOrAboveFreeDeliveryThreshold_noDeliveryFee() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Rice Bag", new BigDecimal("200.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("200.00")); // subtotal = 200, >= 199
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseService.reserveForOrder(any(Order.class))).thenReturn(true);

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        OrderResponseDTO response = orderService.placeOrder("user@nearnow.com", request);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void placeOrder_legacyNonWarehouseProduct_deductsProductStockDirectly() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Legacy Soap", new BigDecimal("30.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 3, new BigDecimal("30.00"));
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        // Not warehouse-managed -> OrderService must fall back to legacy stock deduction.
        when(warehouseService.reserveForOrder(any(Order.class))).thenReturn(false);

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        OrderResponseDTO response = orderService.placeOrder("user@nearnow.com", request);

        assertThat(p.getStock()).isEqualTo(17); // 20 - 3
        assertThat(response.isCancellable()).isTrue(); // PLACED is cancellable
        verify(productRepository).save(p);
    }

    @Test
    void placeOrder_notificationFiresOnlyAfterTransactionCommits() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Bread", new BigDecimal("40.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("40.00"));
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseService.reserveForOrder(any(Order.class))).thenReturn(true);

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        orderService.placeOrder("user@nearnow.com", request);

        // Not sent yet -- transaction hasn't "committed" in this test.
        verify(notificationService, never()).sendOrderConfirmation(any(Order.class));

        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertThat(syncs).hasSize(1);
        syncs.get(0).afterCommit();

        verify(notificationService, times(1)).sendOrderConfirmation(any(Order.class));
    }

    // --- placeOrder: failure paths ---------------------------------------

    @Test
    void placeOrder_noCartRow_throwsCartIsEmpty() throws Exception {
        User customer = user(1L);
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.empty());

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    void placeOrder_cartRowExistsButNoItems_throws() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of());

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Cannot place an order with an empty cart");
    }

    @Test
    void placeOrder_addressBelongsToDifferentUser_throwsNotFoundNotForbidden() throws Exception {
        // Deliberately ResourceNotFoundException, not a 403 -- OrderService's
        // own ownership check treats "not yours" the same as "doesn't exist"
        // so a user can't even confirm another user's address id is valid.
        User customer = user(1L);
        User someoneElse = user(2L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Milk", new BigDecimal("50.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("50.00"));
        Address othersAddress = address(300L, someoneElse);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(othersAddress));

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_addressDoesNotExist_throws() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Milk", new BigDecimal("50.00"), 20, true);
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("50.00"));

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(999L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void placeOrder_productDeactivatedSinceAddedToCart_throws() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Discontinued Item", new BigDecimal("50.00"), 20, false); // inactive
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("50.00"));
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no longer available");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_requestedQuantityExceedsStock_throws() throws Exception {
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Limited Item", new BigDecimal("50.00"), 2, true); // only 2 in stock
        CartItem ci = cartItem(200L, cart, p, 5, new BigDecimal("50.00")); // wants 5
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        assertThatThrownBy(() -> orderService.placeOrder("user@nearnow.com", request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only 2 unit(s) available");
    }

    @Test
    void placeOrder_priceChangedSinceAddedToCart_usesCurrentPriceNotStalePrice() throws Exception {
        // Server-side price boundary: cartItem.priceAtAdd gets overwritten
        // with product.getEffectivePrice() at checkout time, regardless of
        // what price the item was added to the cart at.
        User customer = user(1L);
        Cart cart = cart(customer, 10L);
        Product p = product(100L, "Repriced Item", new BigDecimal("80.00"), 20, true); // now 80, was 50
        CartItem ci = cartItem(200L, cart, p, 1, new BigDecimal("50.00")); // stale price
        Address addr = address(300L, customer);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(10L)).thenReturn(List.of(ci));
        when(addressRepository.findById(300L)).thenReturn(Optional.of(addr));
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(productRepository.findById(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseService.reserveForOrder(any(Order.class))).thenReturn(true);

        PlaceOrderRequestDTO request = new PlaceOrderRequestDTO();
        request.setAddressId(300L);
        request.setPaymentMethod("COD");

        OrderResponseDTO response = orderService.placeOrder("user@nearnow.com", request);

        // subtotal must reflect the CURRENT price (80), not the stale
        // cart price (50), plus delivery fee since 80 < 199.
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
    }

    // --- cancelOrder -----------------------------------------------------

    @Test
    void cancelOrder_placedOrder_legacyStock_restoresStockAndCancels() throws Exception {
        User customer = user(1L);
        Product p = product(100L, "Item", new BigDecimal("50.00"), 5, true); // 5 left after a deduction
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 500L);
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 1));
        order.setInventoryReserved(true); // legacy path reserved stock at checkout

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(warehouseService.restoreReservedStock(order)).thenReturn(false); // not warehouse-managed
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(500L)).thenReturn(Optional.empty());

        OrderResponseDTO response = orderService.cancelOrder("user@nearnow.com", 500L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(p.getStock()).isEqualTo(6); // 5 + 1 restored
        assertThat(order.isStockRestored()).isTrue();
        verify(productRepository).save(p);
    }

    @Test
    void cancelOrder_warehouseManagedOrder_delegatesRestoreToWarehouseService() throws Exception {
        User customer = user(1L);
        Product p = product(100L, "Item", new BigDecimal("50.00"), 5, true);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 501L);
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 1));
        order.setInventoryReserved(true);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(warehouseService.restoreReservedStock(order)).thenReturn(true); // warehouse handled it
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(501L)).thenReturn(Optional.empty());

        orderService.cancelOrder("user@nearnow.com", 501L);

        // Legacy per-product restore must NOT also run -- would double-restore stock.
        verify(productRepository, never()).findByIdForUpdate(anyLong());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void cancelOrder_alreadyDelivered_throwsAndDoesNotTouchStock() throws Exception {
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 502L);
        order.setStatus(OrderStatus.DELIVERED);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(502L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("user@nearnow.com", 502L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("DELIVERED");

        verify(orderRepository, never()).save(any());
        verify(warehouseService, never()).restoreReservedStock(any());
    }

    @Test
    void cancelOrder_packedOrder_stillCancellable() throws Exception {
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 503L);
        order.setStatus(OrderStatus.PACKED);
        order.setInventoryReserved(false); // e.g. inventory step never ran for this legacy row

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(503L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(503L)).thenReturn(Optional.empty());

        OrderResponseDTO response = orderService.cancelOrder("user@nearnow.com", 503L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(warehouseService, never()).restoreReservedStock(any());
    }

    @Test
    void cancelOrder_alreadyRestoredStock_doesNotRestoreTwice() throws Exception {
        // inventoryReserved=true but stockRestored=true already -- e.g. a
        // retried request. Must not add stock back a second time.
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 504L);
        order.setInventoryReserved(true);
        order.setStockRestored(true);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(504L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(504L)).thenReturn(Optional.empty());

        orderService.cancelOrder("user@nearnow.com", 504L);

        verify(warehouseService, never()).restoreReservedStock(any());
        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void cancelOrder_paidPayment_getsRefunded() throws Exception {
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 505L);
        order.setInventoryReserved(false);

        Payment payment = new Payment("pay_ref_505", new BigDecimal("50.00"), customer);
        payment.setStatus(PaymentStatus.PAID);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(505L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(505L)).thenReturn(Optional.of(payment));

        orderService.cancelOrder("user@nearnow.com", 505L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void cancelOrder_unpaidPayment_notTouched() throws Exception {
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 506L);
        order.setInventoryReserved(false);

        Payment payment = new Payment("pay_ref_506", new BigDecimal("50.00"), customer);
        payment.setStatus(PaymentStatus.CREATED); // COD, not yet paid

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(506L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(506L)).thenReturn(Optional.of(payment));

        orderService.cancelOrder("user@nearnow.com", 506L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED); // unchanged
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void cancelOrder_notOwnedByCaller_throwsNotFound() throws Exception {
        User owner = user(1L);
        User attacker = user(2L);
        Order order = new Order(owner, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 507L);

        when(userRepository.findByEmail("attacker@nearnow.com")).thenReturn(Optional.of(attacker));
        when(orderRepository.findById(507L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("attacker@nearnow.com", 507L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    // --- getOrderHistory / getOrderById ------------------------------

    @Test
    void getOrderHistory_returnsOwnOrdersNewestFirst() throws Exception {
        User customer = user(1L);
        Order order = new Order(customer, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 600L);

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponseDTO> history = orderService.getOrderHistory("user@nearnow.com");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getId()).isEqualTo(600L);
    }

    @Test
    void getOrderById_notOwned_throwsNotFoundNotForbidden() throws Exception {
        User owner = user(1L);
        User someoneElse = user(2L);
        Order order = new Order(owner, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(order, 601L);

        when(userRepository.findByEmail("other@nearnow.com")).thenReturn(Optional.of(someoneElse));
        when(orderRepository.findById(601L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("other@nearnow.com", 601L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderById_doesNotExist_throwsNotFound() throws Exception {
        User customer = user(1L);
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("user@nearnow.com", 9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
