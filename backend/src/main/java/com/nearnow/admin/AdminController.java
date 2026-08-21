package com.nearnow.admin;

import com.nearnow.common.dto.ApiResponse;
import com.nearnow.common.dto.PagedResponseDTO;
import com.nearnow.order.Order;
import com.nearnow.auth.User;
import com.nearnow.auth.UserResponseDTO;
import com.nearnow.rider.DeliveryAssignmentResponseDTO;
import com.nearnow.vendor.Vendor;
import com.nearnow.vendor.VendorResponseDTO;
import com.nearnow.warehouse.AdminStoreService;
import com.nearnow.warehouse.StoreRequestDTO;
import com.nearnow.warehouse.StoreResponseDTO;
import com.nearnow.product.Product;
import com.nearnow.product.ProductResponseDTO;
import com.nearnow.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Every endpoint here is ROLE_ADMIN-protected (see SecurityConfig) —
 * the first feature in this project using role-based restriction
 * rather than just "public vs any-logged-in-user." The mechanism
 * (User.role -> JWT claim -> JwtAuthFilter granting "ROLE_" + role)
 * was actually built back in Phase 2, unused until now.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProductService productService;
    private final AdminStoreService adminStoreService;

    public AdminController(AdminService adminService, ProductService productService, AdminStoreService adminStoreService) {
        this.adminService = adminService; this.productService = productService; this.adminStoreService = adminStoreService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDTO>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    // Seed endpoint intentionally lives in its OWN dev-only controller
    // now (AdminSeedController) instead of here — see that file's
    // comment for why a live "seed" button has no place in a
    // production admin panel.

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(@Valid @RequestBody ProductRequestDTO request) {
        Product saved = adminService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductById(saved.getId()), "Product created"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequestDTO request) {
        Product saved = adminService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductById(saved.getId()), "Product updated"));
    }

    @PatchMapping("/products/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateProduct(@PathVariable Long id) {
        adminService.reactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product reactivated"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted"));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProductResponseDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name").ascending());
        Page<Product> products = adminService.getAllProducts(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponseDTO.from(products, products.getContent().stream().map(productService::toDTOForAdmin).toList())));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponseDTO<AdminOrderSummaryDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        Page<AdminOrderSummaryDTO> orders = adminService.getAllOrders(pageable).map(order ->
                new AdminOrderSummaryDTO(order.getId(), order.getUser().getEmail(),
                        order.getTotalAmount(), order.getStatus(), order.getCreatedAt()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponseDTO.from(orders, orders.getContent())));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequestDTO request) {
        adminService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, "Order status updated"));
    }

    /**
     * Elevated roles are provisioned by an administrator, never by public
     * registration. The returned DTO deliberately excludes passwordHash.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponseDTO<UserResponseDTO>>> getUsers(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100), Sort.by("id").descending());
        Page<User> users = adminService.getAllUsers(pageable);
        var mapped = users.getContent().stream().map(u -> new UserResponseDTO(u.getId(),u.getEmail(),u.getFullName(),u.getPhone(),u.getPhotoUrl(),u.getRole(),u.isEmailVerified(),u.isNotificationsEnabled())).toList();
        return ResponseEntity.ok(ApiResponse.success(PagedResponseDTO.from(users,mapped)));
    }

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<PagedResponseDTO<VendorResponseDTO>>> getVendors(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100), Sort.by("id").descending());
        Page<Vendor> vendors = adminService.getAllVendors(pageable);
        var mapped = vendors.getContent().stream().map(v -> new VendorResponseDTO(v.getId(),v.getUser().getId(),v.getUser().getEmail(),v.getBusinessName(),v.getBusinessAddress(),v.getGstNumber(),v.isActive())).toList();
        return ResponseEntity.ok(ApiResponse.success(PagedResponseDTO.from(vendors,mapped)));
    }

    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<PagedResponseDTO<StoreResponseDTO>>> getStores(
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),100), Sort.by("id").descending());
        Page<StoreResponseDTO> stores = adminStoreService.getStores(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponseDTO.from(stores, stores.getContent())));
    }

    @PostMapping("/stores")
    public ResponseEntity<ApiResponse<StoreResponseDTO>> createStore(@Valid @RequestBody StoreRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(adminStoreService.save(null,request),"Store created"));
    }

    @PutMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<StoreResponseDTO>> updateStore(@PathVariable Long id,@Valid @RequestBody StoreRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(adminStoreService.save(id,request),"Store updated"));
    }

    @PatchMapping("/stores/{id}/active")
    public ResponseEntity<ApiResponse<StoreResponseDTO>> setStoreActive(@PathVariable Long id,@RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(adminStoreService.setActive(id,active),"Store status updated"));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponseDTO>> assignRole(
            Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody RoleAssignmentRequestDTO request) {
        User user = adminService.assignRole(authentication.getName(), id, request.getRole());
        UserResponseDTO dto = new UserResponseDTO(
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getPhotoUrl(), user.getRole(), user.isEmailVerified(),
                user.isNotificationsEnabled()
        );
        return ResponseEntity.ok(ApiResponse.success(dto, "Role assigned. User must log in again to receive a JWT with the new role."));
    }

    @PutMapping("/vendors")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> createOrUpdateVendor(
            @Valid @RequestBody AdminVendorRequestDTO request) {
        Vendor vendor = adminService.createOrUpdateVendor(
                request.getUserId(), request.getBusinessName(),
                request.getBusinessAddress(), request.getGstNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(
                new VendorResponseDTO(
                        vendor.getId(), vendor.getUser().getId(), vendor.getUser().getEmail(),
                        vendor.getBusinessName(), vendor.getBusinessAddress(),
                        vendor.getGstNumber(), vendor.isActive()
                ),
                "Vendor profile saved"
        ));
    }

    @PutMapping("/products/{productId}/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<Void>> assignProductToVendor(
            @PathVariable Long productId,
            @PathVariable Long vendorId) {
        adminService.assignProductToVendor(productId, vendorId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product assigned to vendor"));
    }

    @DeleteMapping("/products/{productId}/vendor")
    public ResponseEntity<ApiResponse<Void>> unassignProductFromVendor(@PathVariable Long productId) {
        adminService.unassignProductFromVendor(productId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product unassigned from vendor"));
    }

    @GetMapping("/vendors/{vendorId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getVendorProducts(@PathVariable Long vendorId) {
        List<ProductResponseDTO> products = adminService.getVendorProducts(vendorId).stream()
                .map(productService::toDTOForAdmin)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping("/riders/assign/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDTO>> assignRider(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.assignRider(orderId),
                "Rider assigned"
        ));
    }
}
