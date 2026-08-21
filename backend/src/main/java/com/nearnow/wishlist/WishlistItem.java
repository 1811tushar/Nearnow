package com.nearnow.wishlist;

import com.nearnow.auth.User;
import com.nearnow.product.Product;
import jakarta.persistence.*;

/**
 * One row per (user, product) pair — the relational-normalized form of
 * Firestore's single wishlists/{uid} document with a productIds array
 * field. No separate "Wishlist" header entity, unlike Cart/CartItem —
 * a wishlist has no header-level attribute (no created-date, no status)
 * that Cart's header conceptually could hold, so a wrapper entity here
 * would be unnecessary complexity.
 */
@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        // Relational equivalent of Firestore's arrayUnion's built-in
        // dedup — the DB itself refuses a second (user_id, product_id)
        // row, same reasoning as CartItem's unique constraint.
        @UniqueConstraint(name = "uq_wishlist_user_product", columnNames = {"user_id", "product_id"})
})
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected WishlistItem() {
    }

    public WishlistItem(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }
}
