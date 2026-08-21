package com.nearnow.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PickListRepository extends JpaRepository<PickList, Long> {

    List<PickList> findByStoreIdOrderByIdDesc(Long storeId);

    Optional<PickList> findByOrderId(Long orderId);

    @Query("""
            select p
            from PickList p
            where p.id = :pickListId
              and p.store.warehouseManager.id = :managerId
            """)
    Optional<PickList> findOwnedByManager(
            @Param("pickListId") Long pickListId,
            @Param("managerId") Long managerId
    );
}
