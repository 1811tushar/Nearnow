package com.nearnow.wishlist;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistItemRepository wishlistItemRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public WishlistResponseDTO getWishlist(String userEmail) {
        User user = getUser(userEmail);
        List<Long> ids = wishlistItemRepository.findByUserId(user.getId())
                .stream().map(item -> item.getProduct().getId()).toList();
        return new WishlistResponseDTO(ids);
    }

    // Idempotent by design — matches Firestore's arrayUnion, which
    // silently does nothing if the value is already in the array
    // instead of erroring. This is a deliberate CONTRAST to
    // CartService.addToCart(), which increments quantity on a repeat
    // add — Wishlist has no quantity concept, so "already there" simply
    // means "nothing to do," not "do it again."
    @Transactional
    public WishlistResponseDTO addToWishlist(String userEmail, Long productId) {
        User user = getUser(userEmail);
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        boolean alreadyExists = wishlistItemRepository
                .findByUserIdAndProductId(user.getId(), productId).isPresent();

        if (!alreadyExists) {
            wishlistItemRepository.save(new WishlistItem(user, product));
        }

        return getWishlist(userEmail);
    }

    // Also idempotent — matches Firestore's arrayRemove, which silently
    // does nothing if the value isn't present. Deliberately does NOT
    // throw ResourceNotFoundException (contrast: CartService.removeFromCart
    // DOES throw) — the Flutter provider's optimistic-update+rollback
    // logic (verified in wishlist_provider.dart) expects a benign,
    // always-succeeds contract here.
    @Transactional
    public WishlistResponseDTO removeFromWishlist(String userEmail, Long productId) {
        User user = getUser(userEmail);
        wishlistItemRepository.deleteByUserIdAndProductId(user.getId(), productId);
        return getWishlist(userEmail);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
