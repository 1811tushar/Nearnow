package com.nearnow.cart;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.common.pricing.PricingService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, UserRepository userRepository, PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
    }

    public CartResponseDTO getCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return toDTO(items);
    }

    // @Transactional here is what makes this genuinely a "find-or-
    // increment" operation instead of two separate, race-prone steps —
    // see CartItem's class comment for the full reasoning, including why
    // the unique constraint is still needed as a second layer of defense.
    @Transactional
    public CartResponseDTO addToCart(String userEmail, AddToCartRequestDTO request) {
        Cart cart = getOrCreateCartForUpdate(userEmail);
        if (request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }

        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int resultingQuantity = request.getQuantity() + (existing != null ? existing.getQuantity() : 0);
        if (resultingQuantity > product.getStock()) {
            throw new InvalidOperationException(
                    "Only " + product.getStock() + " unit(s) available for " + product.getName());
        }

        if (existing != null) {
            existing.setQuantity(resultingQuantity);
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = new CartItem(cart, product, request.getQuantity(), product.getEffectivePrice());
            cartItemRepository.save(newItem);
        }

        return getCart(userEmail);
    }

    @Transactional
    public CartResponseDTO removeFromCart(String userEmail, Long cartItemId) {
        Cart cart = getOrCreateCartForUpdate(userEmail);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        // Defensive check: never let one user delete another user's cart
        // item just by guessing an id — a cart-item id alone doesn't
        // prove ownership, only "belongs to a cart matching the caller's
        // own cart id" does.
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item not found: " + cartItemId);
        }

        cartItemRepository.delete(item);
        return getCart(userEmail);
    }

    @Transactional
    public CartResponseDTO updateQuantity(String userEmail, Long cartItemId, int newQuantity) {
        Cart cart = getOrCreateCartForUpdate(userEmail);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item not found: " + cartItemId);
        }

        if (newQuantity <= 0) {
            throw new InvalidOperationException("Quantity must be greater than zero");
        }

        Product product = item.getProduct();
        if (!product.isActive()) {
            throw new InvalidOperationException("This product is no longer available");
        }
        if (newQuantity > product.getStock()) {
            throw new InvalidOperationException(
                    "Only " + product.getStock() + " unit(s) available for " + product.getName());
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return getCart(userEmail);
    }

    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = getOrCreateCartForUpdate(userEmail);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    // Lazily creates a Cart row the first time a user touches their cart
    // — mirrors Firestore's implicit behavior (a carts/{uid} "document"
    // effectively exists the moment you write to its items sub-collection,
    // nothing needs to pre-create it). Relational DB has no such implicit
    // path-creation, so this method makes that same laziness explicit.
    private Cart getOrCreateCartForUpdate(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cartRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private CartResponseDTO toDTO(List<CartItem> items) {
        List<CartItemResponseDTO> itemDTOs = items.stream().map(item -> {
            Product product = item.getProduct();
            String firstImage = product.getImages().isEmpty() ? null : product.getImages().get(0);
            return new CartItemResponseDTO(
                    item.getId(),
                    product.getId(),
                    product.getName(),
                    firstImage,
                    product.getEffectivePrice(),
                    product.getUnit(),
                    item.getQuantity(),
                    product.getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }).toList();

        BigDecimal subtotal = itemDTOs.stream()
                .map(CartItemResponseDTO::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deliveryFee = pricingService.deliveryFee(subtotal);
        BigDecimal grandTotal = pricingService.total(subtotal);
        return new CartResponseDTO(itemDTOs, subtotal, deliveryFee, grandTotal);
    }
}
