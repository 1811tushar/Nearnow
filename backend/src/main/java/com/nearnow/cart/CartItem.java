package com.nearnow.cart;

import com.nearnow.product.Product;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Deliberate design difference from CartItemModel.dart: the Dart model
 * duplicates name/image/price/unit directly onto the cart-item document.
 * That made sense for Firestore — reading a cart shouldn't require N
 * extra reads (one per product) against the free-tier quota, so
 * Firestore denormalizes for read-performance.
 *
 * A relational DB doesn't have that same cost: a JOIN to `products` is
 * cheap and normal. So here, CartItem stores only a reference (`product`)
 * plus `priceAtAdd` — and name/image/unit are read live via the Product
 * join when building the response DTO. `priceAtAdd` is the ONE field
 * still snapshotted, kept deliberately (not an oversight) because it
 * serves a real purpose: comparing it against the product's CURRENT
 * price lets a future checkout step detect "this got repriced since
 * you added it" — same purpose CartService.dart's updatePrice() served.
 */
@Entity
@Table(name = "cart_items", uniqueConstraints = {
        // This is the relational-DB answer to the same problem
        // CartService.dart's own comment described in detail: two
        // simultaneous "add this product" taps must never create two
        // separate line-items for the same product. Firestore solved it
        // by making the product id ALSO the document id (forcing a
        // direct, transaction-safe existence check). Here, the database
        // itself refuses a second (cart_id, product_id) row outright —
        // even if the application-level find-or-create check above it
        // raced and both requests thought "doesn't exist yet", the
        // LOSING insert fails with a constraint violation instead of
        // silently creating a duplicate row.
        @UniqueConstraint(name = "uq_cart_product", columnNames = {"cart_id", "product_id"})
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdd;

    protected CartItem() {
    }

    public CartItem(Cart cart, Product product, int quantity, BigDecimal priceAtAdd) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.priceAtAdd = priceAtAdd;
    }

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtAdd() {
        return priceAtAdd;
    }

    
    public void setPriceAtAdd(BigDecimal priceAtAdd) {
        this.priceAtAdd = priceAtAdd;
    }
    // Java-mirror of CartItemModel's itemTotal getter.
    public BigDecimal getItemTotal() {
        return priceAtAdd.multiply(BigDecimal.valueOf(quantity));
    }
}
