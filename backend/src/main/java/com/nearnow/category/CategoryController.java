package com.nearnow.category;

import com.nearnow.common.dto.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/top-level")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getTopLevel() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTopLevelCategories()));
    }

    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getSubCategories(parentId)));
    }
}
