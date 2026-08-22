package com.nearnow.admin;

import com.nearnow.ai.LocalEmbeddingService;
import com.nearnow.ai.ProductEmbeddingRepository;
import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.category.Category;
import com.nearnow.category.CategoryRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.DeliveryAddressSnapshot;
import com.nearnow.order.Order;
import com.nearnow.order.OrderItem;
import com.nearnow.order.OrderRepository;
import com.nearnow.order.OrderStatus;
import com.nearnow.payment.Payment;
import com.nearnow.payment.PaymentRepository;
import com.nearnow.payment.PaymentStatus;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.rider.RiderService;
import com.nearnow.vendor.Vendor;
import com.nearnow.vendor.VendorRepository;
import com.nearnow.warehouse.StockLevelRepository;
import com.nearnow.warehouse.WarehouseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full unit test coverage of AdminService — every public method.
 *
 * This supersedes an earlier, narrower version of this file that only
 * covered assignProductToVendor/unassignProductFromVendor/getVendorProducts
 * (the roadmap's minimum ask). On review, leaving 8 other methods —
 * including updateOrderStatus, a full order state machine with stock and
 * payment side effects — untested had no real justification: if it's our
 * code, it gets a test, roadmap wording aside.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private LocalEmbeddingService embeddingClient;
    @Mock private ProductEmbeddingRepository productEmbeddingRepository;
    @Mock private UserRepository userRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private StockLevelRepository stockLevelRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private RiderService riderService;
    @Mock private PaymentRepository paymentRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(categoryRepository, productRepository, orderRepository,
                embeddingClient, productEmbeddingRepository, userRepository, vendorRepository,
                stockLevelRepository, warehouseService, riderService, paymentRepository);
    }

    // --- reflection helpers (ids are @GeneratedValue, no public setter) ---

    private static void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private Product product(Long id, String name, boolean active) throws Exception {
        Product p = new Product(name, "desc", null, new BigDecimal("50.00"), "1 unit", 10);
        p.setActive(active);
        setId(p, id);
        return p;
    }

    private Vendor vendor(Long id, boolean active) throws Exception {
        User owner = new User("vendor@nearnow.com", "hash", "Vendor Owner", "");
        setId(owner, 999L);
        owner.setRole("vendor");
        Vendor v = new Vendor(owner, "Test Vendor Store", "123 Business St", "GST123");
        v.setActive(active);
        setId(v, id);
        return v;
    }

    private Category category(Long id, String name) throws Exception {
        Category c = new Category(name, "img.jpg", null, 0);
        setId(c, id);
        return c;
    }

    private User user(Long id, String email, String role) throws Exception {
        User u = new User(email, "hash", "Test User", "");
        u.setRole(role);
        setId(u, id);
        return u;
    }

    private Order order(Long id, User owner, OrderStatus status) throws Exception {
        Order o = new Order(owner, new BigDecimal("50.00"), "COD",
                new DeliveryAddressSnapshot("Home", "Test", "999", "St", "Delhi", "110001", 0, 0));
        setId(o, id);
        o.setStatus(status);
        return o;
    }

    private ProductRequestDTO productRequest(Long categoryId, String name, BigDecimal price,
                                              BigDecimal salePrice, int stock) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setCategoryId(categoryId);
        dto.setName(name);
        dto.setDescription("desc");
        dto.setPrice(price);
        dto.setSalePrice(salePrice);
        dto.setUnit("1 unit");
        dto.setStock(stock);
        return dto;
    }

    // =====================================================================
    // assignProductToVendor / unassignProductFromVendor / getVendorProducts
    // =====================================================================

    @Test
    void assignProductToVendor_activeVendor_linksProductAndSaves() throws Exception {
        Product p = product(1L, "Rice", true);
        Vendor v = vendor(10L, true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(v));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = adminService.assignProductToVendor(1L, 10L);

        assertThat(result.getVendor()).isSameAs(v);
        verify(productRepository).save(p);
    }

    @Test
    void assignProductToVendor_inactiveVendor_throwsAndDoesNotSave() throws Exception {
        Product p = product(1L, "Rice", true);
        Vendor inactiveVendor = vendor(11L, false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(vendorRepository.findById(11L)).thenReturn(Optional.of(inactiveVendor));

        assertThatThrownBy(() -> adminService.assignProductToVendor(1L, 11L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("inactive");

        verify(productRepository, never()).save(any());
        assertThat(p.getVendor()).isNull();
    }

    @Test
    void assignProductToVendor_productDoesNotExist_throwsAndNeverTouchesVendorRepo() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.assignProductToVendor(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(vendorRepository, never()).findById(any());
    }

    @Test
    void assignProductToVendor_vendorDoesNotExist_throws() throws Exception {
        Product p = product(1L, "Rice", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(vendorRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.assignProductToVendor(1L, 404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");

        verify(productRepository, never()).save(any());
    }

    @Test
    void assignProductToVendor_reassigningAlreadyAssignedProduct_overwritesPreviousVendor() throws Exception {
        Product p = product(1L, "Rice", true);
        Vendor vendorA = vendor(10L, true);
        Vendor vendorB = vendor(20L, true);
        p.setVendor(vendorA);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(vendorRepository.findById(20L)).thenReturn(Optional.of(vendorB));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = adminService.assignProductToVendor(1L, 20L);

        assertThat(result.getVendor()).isSameAs(vendorB);
    }

    @Test
    void unassignProductFromVendor_removesLinkAndSaves() throws Exception {
        Product p = product(1L, "Rice", true);
        Vendor v = vendor(10L, true);
        p.setVendor(v);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = adminService.unassignProductFromVendor(1L);

        assertThat(result.getVendor()).isNull();
        verify(productRepository).save(p);
    }

    @Test
    void unassignProductFromVendor_alreadyUnassigned_isIdempotent() throws Exception {
        Product p = product(1L, "Rice", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = adminService.unassignProductFromVendor(1L);

        assertThat(result.getVendor()).isNull();
    }

    @Test
    void unassignProductFromVendor_productDoesNotExist_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.unassignProductFromVendor(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVendorProducts_unknownVendor_throwsBeforeQueryingProducts() {
        when(vendorRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.getVendorProducts(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).findByVendorIdOrderByNameAsc(any());
    }

    @Test
    void getVendorProducts_knownVendor_returnsProducts() throws Exception {
        Product p = product(1L, "Rice", true);
        when(vendorRepository.existsById(10L)).thenReturn(true);
        when(productRepository.findByVendorIdOrderByNameAsc(10L)).thenReturn(List.of(p));

        List<Product> result = adminService.getVendorProducts(10L);

        assertThat(result).containsExactly(p);
    }

    // =====================================================================
    // updateOrderStatus -- the state machine. Highest-risk method in this
    // class: wrong transition rules or a missed stock/payment side effect
    // here means real money or real inventory drifts from reality.
    // =====================================================================

    @Test
    void updateOrderStatus_placedToPacked_allowedNoSideEffects() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(500L, customer, OrderStatus.PLACED);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = adminService.updateOrderStatus(500L, OrderStatus.PACKED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PACKED);
        verify(warehouseService, never()).restoreReservedStock(any());
        verify(paymentRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void updateOrderStatus_packedToOutForDelivery_allowed() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(501L, customer, OrderStatus.PACKED);

        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = adminService.updateOrderStatus(501L, OrderStatus.OUT_FOR_DELIVERY);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
    }

    @Test
    void updateOrderStatus_outForDeliveryToDelivered_allowed() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(502L, customer, OrderStatus.OUT_FOR_DELIVERY);

        when(orderRepository.findById(502L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = adminService.updateOrderStatus(502L, OrderStatus.DELIVERED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatus_placedDirectlyToOutForDelivery_rejectedSkipsPacked() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(503L, customer, OrderStatus.PLACED);

        when(orderRepository.findById(503L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminService.updateOrderStatus(503L, OrderStatus.OUT_FOR_DELIVERY))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("PLACED")
                .hasMessageContaining("OUT_FOR_DELIVERY");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_deliveredIsTerminal_noTransitionAllowed() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(504L, customer, OrderStatus.DELIVERED);

        when(orderRepository.findById(504L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminService.updateOrderStatus(504L, OrderStatus.CANCELLED))
                .isInstanceOf(InvalidOperationException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_sameStatusRequested_isNoOpReturnsUnchanged() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(505L, customer, OrderStatus.PACKED);

        when(orderRepository.findById(505L)).thenReturn(Optional.of(order));

        Order result = adminService.updateOrderStatus(505L, OrderStatus.PACKED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PACKED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_cancelPlacedOrder_legacyStock_restoresStockAndRefunds() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(506L, customer, OrderStatus.PLACED);
        Product p = product(100L, "Item", true); // stock = 10 from helper
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 2));
        order.setInventoryReserved(true);

        Payment payment = new Payment("pay_ref_506", new BigDecimal("50.00"), customer);
        payment.setStatus(PaymentStatus.PAID);

        when(orderRepository.findById(506L)).thenReturn(Optional.of(order));
        when(warehouseService.restoreReservedStock(order)).thenReturn(false); // legacy product
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(506L)).thenReturn(Optional.of(payment));

        Order result = adminService.updateOrderStatus(506L, OrderStatus.CANCELLED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(p.getStock()).isEqualTo(12); // 10 + 2 restored
        assertThat(order.isStockRestored()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void updateOrderStatus_cancelWarehouseManagedOrder_delegatesRestoreToWarehouseService() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(507L, customer, OrderStatus.PACKED);
        Product p = product(100L, "Item", true);
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 2));
        order.setInventoryReserved(true);

        when(orderRepository.findById(507L)).thenReturn(Optional.of(order));
        when(warehouseService.restoreReservedStock(order)).thenReturn(true); // warehouse handled it
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(507L)).thenReturn(Optional.empty());

        adminService.updateOrderStatus(507L, OrderStatus.CANCELLED);

        verify(productRepository, never()).findByIdForUpdate(anyLong());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateOrderStatus_cancelAlreadyStockRestoredOrder_doesNotDoubleRestore() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(508L, customer, OrderStatus.PACKED);
        order.setInventoryReserved(true);
        order.setStockRestored(true); // already restored somehow

        when(orderRepository.findById(508L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(508L)).thenReturn(Optional.empty());

        adminService.updateOrderStatus(508L, OrderStatus.CANCELLED);

        verify(warehouseService, never()).restoreReservedStock(any());
        verify(productRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void updateOrderStatus_cancelUnpaidOrder_paymentNotTouched() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(509L, customer, OrderStatus.PLACED);
        order.setInventoryReserved(false);

        Payment payment = new Payment("pay_ref_509", new BigDecimal("50.00"), customer);
        payment.setStatus(PaymentStatus.CREATED); // COD, not yet paid

        when(orderRepository.findById(509L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(509L)).thenReturn(Optional.of(payment));

        adminService.updateOrderStatus(509L, OrderStatus.CANCELLED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_reactivateCancelledOrder_legacyStock_reReservesStock() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(510L, customer, OrderStatus.CANCELLED);
        Product p = product(100L, "Item", true); // stock = 10
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 3));

        when(orderRepository.findById(510L)).thenReturn(Optional.of(order));
        when(warehouseService.reserveExistingOrder(order)).thenReturn(false); // legacy product
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = adminService.updateOrderStatus(510L, OrderStatus.PLACED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(p.getStock()).isEqualTo(7); // 10 - 3 re-deducted
        assertThat(order.isInventoryReserved()).isTrue();
        assertThat(order.isStockRestored()).isFalse();
        verify(productRepository).save(p);
    }

    @Test
    void updateOrderStatus_reactivateCancelledOrder_insufficientStock_throws() throws Exception {
        User customer = user(1L, "user@nearnow.com", "user");
        Order order = order(511L, customer, OrderStatus.CANCELLED);
        Product p = product(100L, "Item", true); // stock = 10
        order.addItem(new OrderItem(p, "Item", null, new BigDecimal("50.00"), "1 unit", 999)); // way too many

        when(orderRepository.findById(511L)).thenReturn(Optional.of(order));
        when(warehouseService.reserveExistingOrder(order)).thenReturn(false);
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> adminService.updateOrderStatus(511L, OrderStatus.PLACED))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_orderDoesNotExist_throws() {
        when(orderRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateOrderStatus(9999L, OrderStatus.PACKED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =====================================================================
    // createProduct
    // =====================================================================

    @Test
    void createProduct_validRequest_savesTwiceToDeriveBarcodeFromGeneratedId() throws Exception {
        Category cat = category(1L, "Groceries");
        ProductRequestDTO request = productRequest(1L, "New Product", new BigDecimal("99.00"),
                BigDecimal.ZERO, 15);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) setId(p, 42L); // simulate DB assigning an id on first save
            return p;
        });
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        Product result = adminService.createProduct(request);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getBarcode()).isNotBlank();
        verify(productRepository, times(2)).save(any(Product.class));
        verify(productEmbeddingRepository).upsert(42L, new float[]{0.1f, 0.2f});
    }

    @Test
    void createProduct_negativePrice_throwsBeforeAnySave() {
        ProductRequestDTO request = productRequest(1L, "Bad Product", new BigDecimal("-5.00"),
                BigDecimal.ZERO, 10);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Groceries", "img", null, 0)));

        assertThatThrownBy(() -> adminService.createProduct(request))
                .isInstanceOf(InvalidOperationException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_salePriceAboveRegularPrice_throws() {
        ProductRequestDTO request = productRequest(1L, "Bad Product", new BigDecimal("50.00"),
                new BigDecimal("70.00"), 10); // sale price higher than price
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category("Groceries", "img", null, 0)));

        assertThatThrownBy(() -> adminService.createProduct(request))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void createProduct_categoryDoesNotExist_throws() {
        ProductRequestDTO request = productRequest(999L, "Product", new BigDecimal("50.00"),
                BigDecimal.ZERO, 10);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_embeddingServiceThrows_productStillSavedSuccessfully() throws Exception {
        Category cat = category(1L, "Groceries");
        ProductRequestDTO request = productRequest(1L, "New Product", new BigDecimal("99.00"),
                BigDecimal.ZERO, 15);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) setId(p, 43L);
            return p;
        });
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("model unavailable"));

        Product result = adminService.createProduct(request);

        assertThat(result.getId()).isEqualTo(43L); // still succeeded
        verify(productEmbeddingRepository, never()).upsert(any(), any());
    }

    // =====================================================================
    // updateProduct
    // =====================================================================

    @Test
    void updateProduct_validRequest_updatesFieldsAndRegeneratesEmbedding() throws Exception {
        Product existing = product(1L, "Old Name", true);
        Category newCategory = category(2L, "New Category");
        ProductRequestDTO request = productRequest(2L, "New Name", new BigDecimal("60.00"),
                BigDecimal.ZERO, existing.getStock());

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(stockLevelRepository.existsByProductId(1L)).thenReturn(false); // not warehouse-managed
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

        Product result = adminService.updateProduct(1L, request);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPrice()).isEqualByComparingTo("60.00");
        verify(productEmbeddingRepository).upsert(eq(1L), any());
    }

    @Test
    void updateProduct_inactiveProduct_throwsMustReactivateFirst() throws Exception {
        Product inactive = product(1L, "Old Name", false);
        ProductRequestDTO request = productRequest(2L, "New Name", new BigDecimal("60.00"),
                BigDecimal.ZERO, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> adminService.updateProduct(1L, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Reactivate");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_warehouseManagedProduct_stockMismatchThrows() throws Exception {
        Product existing = product(1L, "Item", true); // stock = 10 from helper
        ProductRequestDTO request = productRequest(2L, "Item", new BigDecimal("50.00"),
                BigDecimal.ZERO, 999); // different from current stock of 10

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Cat")));
        when(stockLevelRepository.existsByProductId(1L)).thenReturn(true); // warehouse-managed

        assertThatThrownBy(() -> adminService.updateProduct(1L, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("warehouse-managed");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_warehouseManagedProduct_sameStockValue_allowedNoConflict() throws Exception {
        Product existing = product(1L, "Item", true); // stock = 10
        ProductRequestDTO request = productRequest(2L, "Item", new BigDecimal("50.00"),
                BigDecimal.ZERO, 10); // same as current

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Cat")));
        when(stockLevelRepository.existsByProductId(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

        Product result = adminService.updateProduct(1L, request);

        assertThat(result.getStock()).isEqualTo(10); // untouched by admin, still correct
    }

    @Test
    void updateProduct_productDoesNotExist_throws() {
        ProductRequestDTO request = productRequest(1L, "Name", new BigDecimal("10.00"), BigDecimal.ZERO, 5);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateProduct(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =====================================================================
    // deleteProduct / reactivateProduct
    // =====================================================================

    @Test
    void deleteProduct_activeProduct_softDeletes() throws Exception {
        Product p = product(1L, "Item", true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        adminService.deleteProduct(1L);

        assertThat(p.isActive()).isFalse();
        verify(productRepository).save(p);
    }

    @Test
    void deleteProduct_alreadyInactive_isIdempotentNoSave() throws Exception {
        Product p = product(1L, "Item", false); // already deleted
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        adminService.deleteProduct(1L);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_doesNotExist_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reactivateProduct_inactiveProduct_reactivatesAndRegeneratesEmbedding() throws Exception {
        Product p = product(1L, "Item", false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

        adminService.reactivateProduct(1L);

        assertThat(p.isActive()).isTrue();
        verify(productRepository).save(p);
        verify(productEmbeddingRepository).upsert(eq(1L), any());
    }

    @Test
    void reactivateProduct_doesNotExist_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.reactivateProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =====================================================================
    // assignRole
    // =====================================================================

    @Test
    void assignRole_validRole_updatesAndBumpsAuthVersion() throws Exception {
        User target = user(5L, "target@nearnow.com", "user");
        long versionBefore = target.getAuthVersion();

        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.assignRole("admin@nearnow.com", 5L, "vendor");

        assertThat(result.getRole()).isEqualTo("vendor");
        assertThat(result.getAuthVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void assignRole_sameRoleAlreadySet_noOpDoesNotBumpAuthVersion() throws Exception {
        User target = user(5L, "target@nearnow.com", "vendor");
        long versionBefore = target.getAuthVersion();

        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.assignRole("admin@nearnow.com", 5L, "vendor");

        assertThat(result.getAuthVersion()).isEqualTo(versionBefore);
    }

    @Test
    void assignRole_adminRemovingOwnAdminRole_throws() throws Exception {
        User selfAdmin = user(1L, "admin@nearnow.com", "admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> adminService.assignRole("admin@nearnow.com", 1L, "user"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot remove their own admin role");

        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRole_adminReassigningOwnRoleToAdmin_allowed() throws Exception {
        User selfAdmin = user(1L, "admin@nearnow.com", "admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(selfAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.assignRole("admin@nearnow.com", 1L, "admin");

        assertThat(result.getRole()).isEqualTo("admin");
    }

    @Test
    void assignRole_caseInsensitiveEmailMatch_stillBlocksSelfDemotion() throws Exception {
        User selfAdmin = user(1L, "Admin@NearNow.com", "admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(selfAdmin));

        assertThatThrownBy(() -> adminService.assignRole("admin@nearnow.com", 1L, "user"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void assignRole_unsupportedRole_throws() throws Exception {
        User target = user(5L, "target@nearnow.com", "user");
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.assignRole("admin@nearnow.com", 5L, "superadmin"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Unsupported role");

        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRole_roleCaseAndWhitespaceNormalized() throws Exception {
        User target = user(5L, "target@nearnow.com", "user");
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = adminService.assignRole("admin@nearnow.com", 5L, "  VENDOR  ");

        assertThat(result.getRole()).isEqualTo("vendor");
    }

    @Test
    void assignRole_userDoesNotExist_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.assignRole("admin@nearnow.com", 999L, "user"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =====================================================================
    // createOrUpdateVendor
    // =====================================================================

    @Test
    void createOrUpdateVendor_userHasVendorRole_createsNewProfile() throws Exception {
        User target = user(5L, "vendor@nearnow.com", "vendor");
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(vendorRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = adminService.createOrUpdateVendor(5L, "My Shop", "123 St", "GST999");

        assertThat(result.getBusinessName()).isEqualTo("My Shop");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createOrUpdateVendor_userWithoutVendorRole_throws() throws Exception {
        User target = user(5L, "user@nearnow.com", "user"); // still just "user"
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> adminService.createOrUpdateVendor(5L, "My Shop", "123 St", "GST999"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("role=vendor");

        verify(vendorRepository, never()).save(any());
    }

    @Test
    void createOrUpdateVendor_existingProfile_updatesInPlaceRatherThanDuplicating() throws Exception {
        User target = user(5L, "vendor@nearnow.com", "vendor");
        Vendor existing = vendor(10L, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(vendorRepository.findByUserId(5L)).thenReturn(Optional.of(existing));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = adminService.createOrUpdateVendor(5L, "Updated Shop Name", "New Address", "GST000");

        assertThat(result.getId()).isEqualTo(10L); // same row, not a new one
        assertThat(result.getBusinessName()).isEqualTo("Updated Shop Name");
    }

    @Test
    void createOrUpdateVendor_userDoesNotExist_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createOrUpdateVendor(999L, "Shop", "Addr", "GST"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =====================================================================
    // getDashboard / getAllProducts
    // Thin pass-throughs to repositories -- low individual risk, but a
    // wrong field mapping in getDashboard (e.g. mixing up two counts)
    // would go unnoticed by everything above, so it's still worth a check.
    // =====================================================================

    @Test
    void getDashboard_mapsRepositoryCountsToCorrectFields() {
        when(orderRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(7L);
        when(productRepository.countByActiveTrueAndStockLessThanEqual(5)).thenReturn(3L);
        when(productRepository.countByActiveTrue()).thenReturn(96L);
        when(orderRepository.count()).thenReturn(150L);

        AdminDashboardDTO dashboard = adminService.getDashboard();

        assertThat(dashboard.ordersToday()).isEqualTo(7L);
        assertThat(dashboard.lowStockCount()).isEqualTo(3L);
        assertThat(dashboard.activeProducts()).isEqualTo(96L);
        assertThat(dashboard.totalOrders()).isEqualTo(150L);
    }

    @Test
    void getAllProducts_blankQuery_usesUnfilteredSort() {
        adminService.getAllProducts("  ", Pageable.unpaged());

        verify(productRepository).findAllByOrderByNameAsc(any());
        verify(productRepository, never()).findByNameContainingIgnoreCaseOrderByNameAsc(any(), any());
    }

    @Test
    void getAllProducts_withQuery_usesSearchAndTrims() {
        adminService.getAllProducts("  rice  ", Pageable.unpaged());

        verify(productRepository).findByNameContainingIgnoreCaseOrderByNameAsc(eq("rice"), any());
    }

    // Note: getAllUsers / getAllVendors / getAllOrders are direct one-line
    // pass-throughs to repository.findAllByOrderByIdDesc/findAll -- there's
    // no branching logic to exercise, so a unit test there would only be
    // re-asserting Mockito's own stubbing back at itself. Covered instead
    // by the Postman/integration-test pass since they need a real
    // Pageable + real data to be a meaningful check at all.

    // =====================================================================
    // seedAll
    // =====================================================================

    @Test
    void seedAll_categoriesAlreadyExist_skipsAndDoesNotTouchProducts() {
        when(categoryRepository.findAll()).thenReturn(List.of(new Category("Existing", "img", null, 0)));

        String result = adminService.seedAll();

        assertThat(result).isEqualTo("Already seeded — skipped");
        verify(productRepository, never()).save(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void seedAll_emptyDatabase_seedsCategoriesAndProducts() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            try {
                setId(c, (long) (Math.random() * 100000) + 1);
            } catch (Exception ignored) { /* test helper, never actually fails */ }
            return c;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            try {
                if (p.getId() == null) setId(p, (long) (Math.random() * 100000) + 1);
            } catch (Exception ignored) {}
            return p;
        });
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

        String result = adminService.seedAll();

        // Exact category/product counts come from SeedData's own constant
        // data, which this test deliberately does not hardcode a number
        // for (that would just be re-encoding SeedData's contents here and
        // break every time seed data is edited for unrelated reasons).
        assertThat(result).startsWith("Seeded ");
        assertThat(result).contains("categories");
        assertThat(result).contains("products");
        verify(categoryRepository, atLeastOnce()).save(any(Category.class));
        verify(productRepository, atLeastOnce()).save(any(Product.class));
    }
}
