package com.nearnow.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByActiveTrueOrderByIdAsc();

    Optional<Store> findByWarehouseManagerIdAndActiveTrue(Long userId);
    Optional<Store> findByWarehouseManagerId(Long userId);
}
