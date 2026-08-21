package com.nearnow.category;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // "categories" = cache-name (Redis key-namespace); "top-level" =
    // fixed cache-KEY since this method takes no parameters — every call
    // hits the same cache entry. Read-heavy (every home-page load),
    // write-never (no category-CRUD exists yet — this phase's flagged
    // gap), a structurally safe first caching candidate.
    @Cacheable(value = "categories", key = "'top-level'")
    public List<CategoryResponseDTO> getTopLevelCategories() {
        return categoryRepository.findByParentCategoryIsNullOrderBySortOrder()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // key = "#parentId" — each distinct parentId gets its own cache
    // entry (unlike top-level's single fixed key above).
    @Cacheable(value = "categories", key = "#parentId")
    public List<CategoryResponseDTO> getSubCategories(Long parentId) {
        return categoryRepository.findByParentCategoryIdOrderBySortOrder(parentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Entity -> DTO conversion, kept as one private method so the
    // Entity's internal shape (a Category object reference for parent)
    // only gets flattened to a plain parentCategoryId Long in exactly
    // one place.
    private CategoryResponseDTO toDTO(Category category) {
        Long parentId = category.getParentCategory() != null
                ? category.getParentCategory().getId()
                : null;
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getImageUrl(),
                parentId,
                category.getSortOrder()
        );
    }
}
