package com.nearnow.review;

import com.nearnow.auth.User;
import com.nearnow.product.Product;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        // Relational equivalent of Firestore's {productId}_{userId}
        // composite document-ID trick — one review per user per product,
        // enforced at the DB level.
        @UniqueConstraint(name = "uq_review_product_user", columnNames = {"product_id", "user_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Denormalized snapshot, matching verified source behavior — the
    // displayed reviewer name stays frozen at submission-time even if
    // the user later changes their profile name.
    @Column(nullable = false)
    private String userName;

    // double, NOT BigDecimal — this is a 1-5 star rating, not money.
    // The currency-precision principle (Section 6.1) only applies to
    // fields that represent an amount someone pays or receives.
    @Column(nullable = false)
    private double rating;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Review() {
    }

    public Review(Product product, User user, String userName, double rating, String comment) {
        this.product = product;
        this.user = user;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public User getUser() { return user; }
    public String getUserName() { return userName; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
}
