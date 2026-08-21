package com.nearnow.auth;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * JpaRepository<User, Long> already gives us save(), findById(), findAll(),
 * deleteById(), etc. for free — no method bodies needed, Spring Data JPA
 * generates the implementation at startup.
 *
 * The two methods below don't exist in JpaRepository by default — Spring
 * Data JPA reads the METHOD NAME itself and generates the SQL from it
 * ("findByEmail" -> "SELECT * FROM users WHERE email = ?"). This is why
 * naming these methods precisely matters — the name IS the query.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Used by AuthService.login() to look up a user by the email they typed.
    Optional<User> findByEmail(String email);

    // Used by AuthService.register() for the email-uniqueness check —
    // cheaper than findByEmail().isPresent() since it only asks the DB
    // for a boolean, not a full row.
    boolean existsByEmail(String email);

    Page<User> findAllByOrderByIdDesc(Pageable pageable);
}
