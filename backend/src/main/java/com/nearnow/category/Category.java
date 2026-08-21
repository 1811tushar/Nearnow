package com.nearnow.category;


import jakarta.persistence.*;

/**
 * Self-referencing entity — verified from category_service.dart's
 * fetchTopLevelCategories() (parentCategoryId IS NULL) and
 * fetchSubCategories() (parentCategoryId == some id). A Category can
 * optionally point to another Category as its parent — that's what
 * "self-referencing" means: the foreign key points back to this same table.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    // The self-reference. Nullable = this is a top-level category.
    // Non-null = this is a sub-category, pointing at its parent.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @Column(nullable = false)
    private int sortOrder;

    protected Category() {
    }

    public Category(String name, String imageUrl, Category parentCategory, int sortOrder) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.parentCategory = parentCategory;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(Category parentCategory) {
        this.parentCategory = parentCategory;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
