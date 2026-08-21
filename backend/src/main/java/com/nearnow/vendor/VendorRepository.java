package com.nearnow.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByUserId(Long userId);

    Optional<Vendor> findByUserEmail(String email);

    Page<Vendor> findAllByOrderByIdDesc(Pageable pageable);
}
