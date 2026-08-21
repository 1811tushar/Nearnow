package com.nearnow.product;


import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Pageable = Spring's built-in pagination object (page number, page
    // size, sort order all bundled together) — replaces the manual
    // Firestore DocumentSnapshot-cursor logic from ProductService.dart's
    // ProductPage class entirely. Page<Product> comes back already
    // knowing hasNext/totalElements/etc — we don't hand-roll a "hasMore"
    // boolean like the old code did.
    Page<Product> findByActiveTrueOrderByNameAsc(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrueOrderByNameAsc(Long categoryId, Pageable pageable);

    // Unsorted variants — the ORDER now comes dynamically from the
    // Pageable's Sort object (built in ProductController from the
    // ?sort= query param), instead of being hardcoded into the method
    // name like the *OrderByNameAsc methods above.
    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    List<Product> findByIsFeaturedTrueAndActiveTrue();

    long countByActiveTrueAndStockLessThanEqual(int stock);
       long countByActiveTrue();
    // Optional<Product> is used here (not in the interface signature
    // directly, but by Spring's convention) — barcode should be unique
    // in practice, so a single result (or none) is the expected shape.
    Product findByBarcodeAndActiveTrue(String barcode);

    java.util.Optional<Product> findByIdAndActiveTrue(Long id);

    // Spring Data JPA translates "In" directly to an SQL "WHERE id IN
    // (...)" — this REPLACES the old getProductsByIds()'s manual 30-item
    // chunking entirely. That chunking existed only because Firestore's
    // whereIn operator caps at 30 items; a real SQL "IN" clause has no
    // such limit (practically, thousands of IDs work fine) — this is a
    // capability the relational database gives us for free that
    // Firestore didn't.
    List<Product> findByIdInAndActiveTrue(List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // NEW — didn't exist in the old ProductService at all (Migration
    // Table will flag this). "ContainingIgnoreCase" generates a
    // case-insensitive SQL LIKE '%text%' query — this is what makes
    // search work across the WHOLE catalog server-side, not just
    // whatever page happened to already be loaded on the client.
    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String query, Pageable pageable);

    List<Product> findByVendorIdOrderByNameAsc(Long vendorId);

    Page<Product> findAllByOrderByNameAsc(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String query, Pageable pageable);
}
