package com.nearnow.cart;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    // Used by addToCart's find-or-increment check — the application-level
    // half of the atomicity guarantee, backed by the DB-level unique
    // constraint on CartItem for the case this check races.
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);
}
