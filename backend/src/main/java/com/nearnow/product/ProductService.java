package com.nearnow.product;

import com.nearnow.common.dto.PagedResponseDTO;
import com.nearnow.common.exception.ResourceNotFoundException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

         public PagedResponseDTO<ProductResponseDTO> getProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByActiveTrue(pageable);
        return PagedResponseDTO.from(page, page.getContent().stream().map(this::toDTO).toList());
    }

    public PagedResponseDTO<ProductResponseDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> page = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        return PagedResponseDTO.from(page, page.getContent().stream().map(this::toDTO).toList());
    }
    

    public List<ProductResponseDTO> getProductsByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        // No manual 30-item chunking loop here — see ProductRepository's
        // findByIdIn comment for why the old chunking logic doesn't
        // carry over.
        return productRepository.findByIdInAndActiveTrue(ids).stream().map(this::toDTO).toList();
    }

    // IMPORTANT: ReviewService.submitReview() mutates product.rating/
    // reviewCount directly (Phase 9). Without evicting THIS cache entry
    // when that happens, a cached product would silently show a stale
    // rating after every review — see ReviewService for the matching
    // @CacheEvict that keeps this honest.
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return toDTO(product);
    }

    @Cacheable(value = "products", key = "'featured'")
    public List<ProductResponseDTO> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndActiveTrue().stream().map(this::toDTO).toList();
    }

    public ProductResponseDTO getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeAndActiveTrue(barcode);
        if (product == null) {
            throw new ResourceNotFoundException("No product found for barcode: " + barcode);
        }
        return toDTO(product);
    }

    // NEW METHOD — see ProductRepository.findByNameContainingIgnoreCase's
    // comment. This is what makes image-search (and any future manual
    // search box) search the ENTIRE catalog, not just the currently
    // loaded page — closing the gap flagged after verifying
    // product_list_page.dart's client-side-only filtering.
    public PagedResponseDTO<ProductResponseDTO> searchProducts(String query, Pageable pageable) {
        Page<Product> page = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable);
        return PagedResponseDTO.from(page, page.getContent().stream().map(this::toDTO).toList());
    }

    public ProductResponseDTO toDTOForAdmin(Product product) { return toDTO(product); }

    private ProductResponseDTO toDTO(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                categoryId,
                product.getImages(),
                product.getPrice(),
                product.getSalePrice(),
                product.getEffectivePrice(),
                product.getDiscountPercent(),
                product.getUnit(),
                product.getStock(),
                product.getRating(),
                product.isFeatured(),
                product.getBarcode(),
                product.getReviewCount(),
                product.isActive()
        );
    }
}
