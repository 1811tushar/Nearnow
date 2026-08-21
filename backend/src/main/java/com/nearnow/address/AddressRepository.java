package com.nearnow.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);

    // @Modifying + @Query: a hand-written bulk UPDATE, because Spring
    // Data JPA's method-name-derived queries only generate SELECTs by
    // default. This is the relational equivalent of Firestore's
    // batch.update() loop in setDefaultAddress() — one statement,
    // touches every address row for this user at once, instead of
    // loading each Address object into memory and saving it back
    // one-by-one.
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);
}
