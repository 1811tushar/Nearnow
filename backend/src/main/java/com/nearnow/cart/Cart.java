package com.nearnow.cart;

import com.nearnow.auth.User;

import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One cart per user — mirrors Firestore's carts/{uid} document path
    // (one cart-document per user, items as a sub-collection under it).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }
}
