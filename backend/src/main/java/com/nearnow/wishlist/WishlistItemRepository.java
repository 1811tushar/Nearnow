package com.nearnow.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserId(Long userId);

    // Used by addToWishlist() to check "is this already there?" before
    // deciding whether to insert — this is what makes add idempotent
    // (matching Firestore's arrayUnion behavior), same pattern as
    // CartItemRepository.findByCartIdAndProductId.
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
