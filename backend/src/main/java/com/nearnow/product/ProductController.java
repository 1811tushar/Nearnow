package com.nearnow.product;

import com.nearnow.ai.SemanticSearchService;
import com.nearnow.common.dto.ApiResponse;
import com.nearnow.common.dto.PagedResponseDTO;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SemanticSearchService semanticSearchService;

    public ProductController(ProductService productService, SemanticSearchService semanticSearchService) {
        this.productService = productService;
        this.semanticSearchService = semanticSearchService;
    }

   
       // sort accepts: name_asc (default), price_asc, price_desc, rating_desc
    // — buildSort() below is the single place that turns that string
    // into a real Sort object, same "one place for a repeated concern"
    // discipline as GlobalExceptionHandler/SecurityConfig.
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProductResponseDTO>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), buildSort(sort));
        return ResponseEntity.ok(ApiResponse.success(productService.getProducts(pageable)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProductResponseDTO>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), buildSort(sort));
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategory(categoryId, pageable)));
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating_desc" -> Sort.by("rating").descending();
            default -> Sort.by("name").ascending();
        };
    }
    

    // ?ids=1,2,3 — replaces getProductsByIds' 30-item Firestore chunking.
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getBatch(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByIds(ids)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeaturedProducts()));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductByBarcode(barcode)));
    }

    // NEW endpoint — see ProductService.searchProducts()'s comment.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProductResponseDTO>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(q, pageable)));
    }

    // Phase 13 — semantic/natural-language search. Deliberately a
    // SEPARATE endpoint from /search above, not a replacement — keyword
    // search (fast, exact, no API-call cost) stays the default; this is
    // for queries keyword-matching genuinely can't handle ("something
    // for a headache"). Not paginated like /search — a ranked top-N
    // result list is the natural shape here, not a browsable full list.
    @GetMapping("/semantic-search")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> semanticSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        if (q == null || q.isBlank() || q.length() > 120) {
            throw new com.nearnow.common.exception.InvalidOperationException("Search query must be 1-120 characters");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return ResponseEntity.ok(ApiResponse.success(semanticSearchService.search(q.trim(), safeLimit)));
    }
}
