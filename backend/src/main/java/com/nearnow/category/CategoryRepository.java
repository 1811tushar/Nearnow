package com.nearnow.category;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Java Spring Data JPA reads "IsNull" on a relationship field
    // (parentCategory) and generates "WHERE parent_category_id IS NULL" —
    // direct mirror of fetchTopLevelCategories()'s Firestore query.
    List<Category> findByParentCategoryIsNullOrderBySortOrder();

    // "ParentCategoryId" here means "the id field of the related
    // parentCategory object" — Spring Data JPA can traverse relationships
    // in the method name itself, no need to write a query by hand.
    List<Category> findByParentCategoryIdOrderBySortOrder(Long parentCategoryId);
}
