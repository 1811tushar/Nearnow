package com.nearnow.common.dto;

import com.nearnow.product.Product;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring's own Page<T> object, if returned directly from a Controller,
 * serializes into JSON with a dozen Spring-internal fields (pageable,
 * sort, numberOfElements, empty, etc.) that leak implementation detail
 * to the client and clutter Flutter's fromJson. This wraps just what
 * Flutter actually needs — the same "define our own contract, don't
 * expose the framework's internal shape" discipline as ApiResponse<T>
 * from Phase 1.
 */
public class PagedResponseDTO<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;
    private int totalPages;

    public PagedResponseDTO(List<T> content, int page, int size, long totalElements, boolean hasMore, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.hasMore = hasMore;
        this.totalPages = totalPages;
    }

    // Convenience factory — builds this DTO straight from a Spring
    // Page<Product> plus the already-mapped list of DTOs, so Service
    // methods don't repeat this field-by-field construction every time.
    public static <T> PagedResponseDTO<T> from(Page<?> springPage, List<T> mappedContent) {
        return new PagedResponseDTO<>(
                mappedContent,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.hasNext(),
                springPage.getTotalPages()
        );
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isHasMore() { return hasMore; }

    public int getTotalPages() { return totalPages; }
}
