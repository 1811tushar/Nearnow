package com.nearnow.rider;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {

    Optional<Rider> findByUserId(Long userId);

    Optional<Rider> findByUserEmail(String email);

    List<Rider> findByActiveTrueAndAvailableTrueOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rider r where r.id = :id")
    Optional<Rider> findByIdForUpdate(@Param("id") Long id);
}
