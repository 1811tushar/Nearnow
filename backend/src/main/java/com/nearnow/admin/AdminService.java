package com.nearnow.admin;

import com.nearnow.ai.LocalEmbeddingService;
import com.nearnow.ai.ProductEmbeddingRepository;
import com.nearnow.category.Category;
import com.nearnow.category.CategoryRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.Order;
import com.nearnow.order.OrderRepository;
import com.nearnow.order.OrderStatus;
import com.nearnow.order.OrderItem;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.rider.RiderService;
import com.nearnow.payment.PaymentRepository;
import com.nearnow.payment.PaymentStatus;
import com.nearnow.rider.DeliveryAssignmentResponseDTO;
import com.nearnow.vendor.Vendor;
import com.nearnow.vendor.VendorRepository;
import com.nearnow.warehouse.StockLevelRepository;
import com.nearnow.warehouse.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.time.*;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final LocalEmbeddingService embeddingClient;
    private final ProductEmbeddingRepository productEmbeddingRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final StockLevelRepository stockLevelRepository;
    private final WarehouseService warehouseService;
    private final RiderService riderService;
    private final PaymentRepository paymentRepository;

    public AdminService(CategoryRepository categoryRepository, ProductRepository productRepository,
                         OrderRepository orderRepository, LocalEmbeddingService embeddingClient,
                         ProductEmbeddingRepository productEmbeddingRepository,
                         UserRepository userRepository, VendorRepository vendorRepository,
                         StockLevelRepository stockLevelRepository, WarehouseService warehouseService,
                         RiderService riderService, PaymentRepository paymentRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.embeddingClient = embeddingClient;
        this.productEmbeddingRepository = productEmbeddingRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.warehouseService = warehouseService;
        this.riderService = riderService;
        this.paymentRepository = paymentRepository;
    }

    // Local embedding generation is deterministic and offline. It never calls
    // a paid external AI service and therefore remains safe on the critical
    // product-management path.
    private void tryGenerateEmbedding(Product product) {
        try {
            float[] embedding = embeddingClient.embed(product.getName() + " " + product.getDescription());
            productEmbeddingRepository.upsert(product.getId(), embedding);
        } catch (Exception e) {
            log.warn("Embedding generation skipped for product #{}: {}", product.getId(), e.getMessage());
        }
    }

    // Idempotency check mirrors isAlreadySeeded() — same "safe regardless
    // of caller discipline" reasoning as the verified Firestore version:
    // the check is enforced INSIDE this method, not left as a doc-comment
    // convention for callers to remember.
    //
    // The old ~108+ sequential-write, chunked-batch-commit dance existed
    // specifically to work around Firestore's 500-operation batch cap —
    // that constraint doesn't exist here. saveAll() is one call; Postgres
    // has no equivalent operation-count ceiling at this data size, so the
    // chunking logic is dropped entirely (same pattern as findByIdIn
    // dropping Firestore's whereIn 30-item chunking back in Phase 4).
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "products", allEntries = true)
    })
    public String seedAll() {
        if (!categoryRepository.findAll().isEmpty()) {
            return "Already seeded — skipped";
        }

        Map<String, Category> categoryByName = new HashMap<>();
        for (SeedData.CategorySeed cs : SeedData.SEED_CATEGORIES) {
            Category category = categoryRepository.save(
                    new Category(cs.name(), cs.imageUrl(), null, cs.sortOrder()));
            categoryByName.put(cs.name(), category);
        }

        int productCount = 0;
        for (Map.Entry<String, List<SeedData.ProductSeed>> entry : SeedData.SEED_PRODUCTS_BY_CATEGORY.entrySet()) {
            Category category = categoryByName.get(entry.getKey());
            if (category == null) continue;

            for (SeedData.ProductSeed ps : entry.getValue()) {
                Product product = new Product(
                        ps.name(), ps.name() + " — fresh and quality checked.",
                        category, ps.price(), ps.unit(), ps.stock()
                );
                product.setSalePrice(ps.salePrice());
                product.setRating(ps.rating());
                product.setFeatured(ps.isFeatured());
                product.setImages(new ArrayList<>(List.of(ps.imageUrl())));
                Product saved = productRepository.save(product);
                // Same hash-based-from-id barcode generator as the
                // verified Firestore version — stable, deterministic,
                // no external barcode-database dependency needed for
                // seed/demo data.
                saved.setBarcode(generateBarcodeFromId(saved.getId()));
                productRepository.save(saved);
                tryGenerateEmbedding(saved);
                productCount++;
            }
        }

        return "Seeded " + categoryByName.size() + " categories, " + productCount + " products";
    }

    private void validatePrice(java.math.BigDecimal price, java.math.BigDecimal salePrice) {
        if (price == null || price.signum() < 0) throw new InvalidOperationException("Price must be zero or greater");
        if (salePrice != null && salePrice.signum() < 0) throw new InvalidOperationException("Sale price must be zero or greater");
        if (salePrice != null && salePrice.signum() > 0 && salePrice.compareTo(price) > 0) {
            throw new InvalidOperationException("Sale price cannot be greater than product price");
        }
    }

    private String generateBarcodeFromId(Long id) {
        long code = Math.abs(id.hashCode()) % 900_000_000_000L;
        return String.valueOf(100_000_000_000L + code);
    }

    // Evicts the 'featured' list cache — a newly-created product could
    // be featured=true, which would otherwise be silently absent from
    // the cached featured-list until it happened to expire on its own.
    @Transactional
    @CacheEvict(value = "products", key = "'featured'")
    public Product createProduct(ProductRequestDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        validatePrice(request.getPrice(), request.getSalePrice());
        Product product = new Product(request.getName().trim(), request.getDescription(), category,
                request.getPrice(), request.getUnit(), request.getStock());
        product.setSalePrice(request.getSalePrice() == null ? java.math.BigDecimal.ZERO : request.getSalePrice());
        product.setFeatured(request.isFeatured());
        // Barcode is a server-owned identifier. Save once to obtain
        // the DB id, then derive the barcode deterministically from that id.
        if (request.getImages() != null) product.setImages(new ArrayList<>(request.getImages()));

        Product saved = productRepository.save(product);
        saved.setBarcode(generateBarcodeFromId(saved.getId()));
        saved = productRepository.save(saved);
        tryGenerateEmbedding(saved);
        return saved;
    }

    // Evicts THIS product's own cached entry (its price/name/etc may
    // have just changed) AND the featured-list (featured status may
    // have flipped either direction).
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "products", key = "'featured'")
    })
    public Product updateProduct(Long productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new InvalidOperationException("Product is inactive. Reactivate it before editing.");
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        validatePrice(request.getPrice(), request.getSalePrice());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice() == null ? java.math.BigDecimal.ZERO : request.getSalePrice());
        product.setUnit(request.getUnit());

        if (stockLevelRepository.existsByProductId(productId)
                && request.getStock() != product.getStock()) {
            throw new InvalidOperationException(
                    "This product is warehouse-managed. Update stock through the warehouse stock endpoint."
            );
        }
        if (!stockLevelRepository.existsByProductId(productId)) {
            product.setStock(request.getStock());
        }
        product.setFeatured(request.isFeatured());
        if (request.getImages() != null) product.setImages(request.getImages());

        Product saved = productRepository.save(product);
        // Name/description may have changed — the embedding must be
        // regenerated, not just left stale, or semantic-search would
        // keep matching this product on its OLD content indefinitely.
        tryGenerateEmbedding(saved);
        return saved;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if (!product.isActive()) {
            return; // idempotent soft-delete
        }

        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true)
    })
    public void reactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.setActive(true);
        productRepository.save(product);
        tryGenerateEmbedding(product);
    }

    public Page<User> getAllUsers(Pageable pageable) { return userRepository.findAllByOrderByIdDesc(pageable); }

    public Page<Vendor> getAllVendors(Pageable pageable) { return vendorRepository.findAllByOrderByIdDesc(pageable); }

    public Page<Product> getAllProducts(String query, Pageable pageable) {
        return query == null || query.isBlank()
                ? productRepository.findAllByOrderByNameAsc(pageable)
                : productRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim(), pageable);
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public AdminDashboardDTO getDashboard() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        return new AdminDashboardDTO(
                orderRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                productRepository.countByActiveTrueAndStockLessThanEqual(5),
                productRepository.countByActiveTrue(),
                orderRepository.count());
    }

    // Deliberately DIFFERENT authorization rule from OrderService's
    // customer-facing cancelOrder() (Section 6.4's asymmetric-authorization
    // pattern) — an admin can transition an order to ANY status, not just
    // PLACED/PACKED -> CANCELLED. Customer-facing and admin-facing actions
    // on the same entity legitimately have different rules; this is not
    // a contradiction of cancelOrder()'s restriction, it's a different
    // actor with different authority.
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == newStatus) {
            return order;
        }

        if (!isAllowedTransition(oldStatus, newStatus)) {
            throw new InvalidOperationException("Invalid order transition: " + oldStatus + " -> " + newStatus);
        }
        if (oldStatus != OrderStatus.CANCELLED && newStatus == OrderStatus.CANCELLED) {
            if (order.isInventoryReserved() && !order.isStockRestored()) {
                boolean warehouseManaged = warehouseService.restoreReservedStock(order);
                if (!warehouseManaged) restoreStock(order);
                order.setStockRestored(true);
            }
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                }
            });
        } else if (oldStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            boolean warehouseManaged = warehouseService.reserveExistingOrder(order);
            if (!warehouseManaged) {
                reserveStock(order);
            }
            order.setInventoryReserved(true);
            order.setStockRestored(false);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private boolean isAllowedTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PLACED -> to == OrderStatus.PACKED || to == OrderStatus.CANCELLED;
            case PACKED -> to == OrderStatus.OUT_FOR_DELIVERY || to == OrderStatus.CANCELLED;
            case OUT_FOR_DELIVERY -> to == OrderStatus.DELIVERED;
            case DELIVERED -> false;
            case CANCELLED -> to == OrderStatus.PLACED;
        };
    }

    @Transactional
    public User assignRole(String adminEmail, Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        if (user.getEmail().equalsIgnoreCase(adminEmail) && !"admin".equals(normalizedRole)) {
            throw new InvalidOperationException("An administrator cannot remove their own admin role");
        }
        if (!List.of("user", "admin", "warehouse_manager", "vendor", "rider").contains(normalizedRole)) {
            throw new InvalidOperationException(
                    "Unsupported role. Allowed roles: user, admin, warehouse_manager, vendor, rider"
            );
        }

        if (!normalizedRole.equalsIgnoreCase(user.getRole())) {
            user.setRole(normalizedRole);
            user.incrementAuthVersion();
        }
        return userRepository.save(user);
    }

    @Transactional
    public Vendor createOrUpdateVendor(Long userId, String businessName,
                                       String businessAddress, String gstNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (!"vendor".equalsIgnoreCase(user.getRole())) {
            throw new InvalidOperationException("User must have role=vendor before a vendor profile is created");
        }

        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseGet(() -> new Vendor(user, businessName, businessAddress, gstNumber));
        vendor.setBusinessName(businessName);
        vendor.setBusinessAddress(businessAddress);
        vendor.setGstNumber(gstNumber);
        vendor.setActive(true);
        return vendorRepository.save(vendor);
    }

    @Transactional
    public Product assignProductToVendor(Long productId, Long vendorId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));
        if (!vendor.isActive()) {
            throw new InvalidOperationException("Vendor account is inactive");
        }
        product.setVendor(vendor);
        return productRepository.save(product);
    }

    @Transactional
    public Product unassignProductFromVendor(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.setVendor(null);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getVendorProducts(Long vendorId) {
        if (!vendorRepository.existsById(vendorId)) {
            throw new ResourceNotFoundException("Vendor not found: " + vendorId);
        }
        return productRepository.findByVendorIdOrderByNameAsc(vendorId);
    }

    @Transactional
    public DeliveryAssignmentResponseDTO assignRider(Long orderId) {
        return riderService.autoAssignOrder(orderId);
    }

    private void restoreStock(Order order) {
        order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .forEach(item -> {
                    Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found: " + item.getProduct().getId()));
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                });
    }

    private void reserveStock(Order order) {
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        Map<Long, Product> lockedProducts = new HashMap<>();
        for (OrderItem item : items) {
            lockedProducts.put(item.getProduct().getId(),
                    productRepository.findByIdForUpdate(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found: " + item.getProduct().getId())));
        }

        for (OrderItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (product.getStock() < item.getQuantity()) {
                throw new InvalidOperationException(
                        "Cannot reactivate order #" + order.getId() + ": insufficient stock for " + product.getName());
            }
        }

        for (OrderItem item : items) {
            Product product = lockedProducts.get(item.getProduct().getId());
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }
}