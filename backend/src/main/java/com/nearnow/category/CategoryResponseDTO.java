package com.nearnow.category;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryResponseDTO {

    private Long id;
    private String name;
    private String imageUrl;
    private Long parentCategoryId; // null for top-level categories
    private int sortOrder;


            @JsonCreator
    public CategoryResponseDTO(@JsonProperty("id") Long id, @JsonProperty("name") String name,
                                @JsonProperty("imageUrl") String imageUrl, @JsonProperty("parentCategoryId") Long parentCategoryId,
                                @JsonProperty("sortOrder") int sortOrder) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.parentCategoryId = parentCategoryId;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
