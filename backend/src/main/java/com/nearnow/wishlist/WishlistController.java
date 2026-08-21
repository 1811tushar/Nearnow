package com.nearnow.wishlist;

import com.nearnow.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(authentication.getName())));
    }

    @PostMapping("/add/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> addToWishlist(
            Authentication authentication, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.addToWishlist(authentication.getName(), productId), "Added to wishlist"));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponseDTO>> removeFromWishlist(
            Authentication authentication, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.removeFromWishlist(authentication.getName(), productId), "Removed from wishlist"));
    }
}
