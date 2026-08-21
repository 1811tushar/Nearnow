package com.nearnow.vendor;

import com.nearnow.common.dto.ApiResponse;
import com.nearnow.common.dto.PagedResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorService.getProfile(authentication.getName())
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody VendorProfileRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorService.updateProfile(authentication.getName(), request),
                "Vendor profile updated"
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<VendorProductResponseDTO>>> getProducts(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorService.getProducts(authentication.getName())
        ));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<VendorProductResponseDTO>> updateProduct(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody VendorProductUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorService.updateProduct(authentication.getName(), id, request),
                "Product updated"
        ));
    }

    @PostMapping("/products/{id}/restock-request")
    public ResponseEntity<ApiResponse<RestockRequestResponseDTO>> createRestockRequest(Authentication authentication,@PathVariable Long id,@Valid @RequestBody RestockRequestDTO request){
        return ResponseEntity.ok(ApiResponse.success(vendorService.createRestockRequest(authentication.getName(),id,request),"Restock request created"));
    }

    @GetMapping("/restock-requests")
    public ResponseEntity<ApiResponse<List<RestockRequestResponseDTO>>> getRestockRequests(Authentication authentication){
        return ResponseEntity.ok(ApiResponse.success(vendorService.getRestockRequests(authentication.getName())));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PagedResponseDTO<VendorOrderResponseDTO>>> getOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("createdAt").descending());
        Page<VendorOrderResponseDTO> orders = vendorService.getOrders(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponseDTO.from(orders, orders.getContent())));
    }
}
