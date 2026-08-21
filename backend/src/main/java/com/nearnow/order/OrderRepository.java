package com.nearnow.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtGreaterThanEqual(Instant start);



    /**
 * Vendors need order visibility only for orders containing one of their
 * products. DISTINCT prevents duplicate Order rows when an order contains
 * multiple products owned by the same vendor.
 *
 * Deliberately no ORDER BY here. Spring Data JPA appends the Pageable's
 * Sort clause to whatever this @Query already contains rather than
 * replacing it — an explicit ORDER BY in the JPQL plus a Sort on the
 * incoming Pageable produces a second, duplicate ORDER BY and Hibernate
 * throws a query-syntax exception at call time. Sorting is the caller's
 * concern (VendorController builds the Pageable with
 * Sort.by("createdAt").descending()); this query only defines the filter.
 */
@Query("""
        select distinct o
        from Order o
        join o.items oi
        join oi.product p
        where p.vendor.id = :vendorId
        """)
Page<Order> findDistinctOrdersContainingVendor(
        @Param("vendorId") Long vendorId, Pageable pageable);
    
}
