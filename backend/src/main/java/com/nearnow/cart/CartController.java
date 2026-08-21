package com.nearnow.cart;

import com.nearnow.common.config.SecurityConfig;
import com.nearnow.common.dto.ApiResponse;
import com.nearnow.common.security.JwtAuthFilter;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Every method here takes an `Authentication` parameter — none of these
 * endpoints are in SecurityConfig's permitAll list, so by the time any
 * method body runs, JwtAuthFilter has already validated the token and
 * populated this object. authentication.getName() is always the caller's
 * OWN email — there is no "whose cart" parameter anywhere in these URLs,
 * because a JWT-authenticated request can only ever act on its own cart.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(authentication.getName())));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.addToCart(authentication.getName(), request), "Added to cart"));
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> removeFromCart(
            Authentication authentication,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.removeFromCart(authentication.getName(), cartItemId), "Removed from cart"));
    }

    @PutMapping("/update-qty/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateQuantity(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateQuantityRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateQuantity(authentication.getName(), cartItemId, request.getQuantity())));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared"));
    }
}
