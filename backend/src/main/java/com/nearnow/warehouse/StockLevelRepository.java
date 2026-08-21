package com.nearnow.warehouse;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByStoreIdAndProductId(Long storeId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sl
            from StockLevel sl
            where sl.store.id = :storeId
              and sl.product.id = :productId
            """)
    Optional<StockLevel> findByStoreIdAndProductIdForUpdate(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId
    );

    List<StockLevel> findByStoreIdOrderByProduct_NameAsc(Long storeId);

    List<StockLevel> findByStoreIdAndProductIdIn(Long storeId, List<Long> productIds);

    /**
     * Locks the rows used to make a checkout allocation decision.
     * Without this lock, two concurrent checkouts could both observe
     * the same remaining quantity before either decrements it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select sl
            from StockLevel sl
            where sl.store.id = :storeId
              and sl.product.id in :productIds
            """)
    List<StockLevel> findByStoreIdAndProductIdInForUpdate(
            @Param("storeId") Long storeId,
            @Param("productIds") List<Long> productIds
    );

    /**
     * Atomic SQL decrement. The quantity guard is a second safety net
     * in addition to the row lock.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update StockLevel sl
            set sl.quantity = sl.quantity - :quantity
            where sl.store.id = :storeId
              and sl.product.id = :productId
              and sl.quantity >= :quantity
            """)
    int decrementQuantity(
            @Param("storeId") Long storeId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    boolean existsByProductId(Long productId);

    boolean existsByStoreIdAndProductId(Long storeId, Long productId);

    @Query("""
            select coalesce(sum(sl.quantity), 0)
            from StockLevel sl
            where sl.product.id = :productId
            """)
    long sumQuantityByProductId(@Param("productId") Long productId);
}
