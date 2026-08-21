package com.nearnow.auth;


import jakarta.persistence.*;
import java.time.Instant;

/**
 * Java-side mirror of Flutter's UserModel (core/models/user_model.dart),
 * with two deliberate differences from the Firestore version:
 *
 * 1. `passwordHash` is new — Firebase Auth used to handle passwords
 *    invisibly, outside our own database. Now WE are the identity
 *    provider, so we store a hashed password ourselves.
 * 2. `savedAddresses` (List<String> in UserModel) is INTENTIONALLY
 *    absent here — it normalizes into its own Address table in
 *    Phase 7, linked back to User via @OneToMany. Keeping a list of
 *    strings inside User was fine for a Firestore document; it's the
 *    wrong shape for a relational table (see Concept Glossary).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Never sent to the client — DTOs (Step 3) are what leave this
    // boundary. This field only ever travels Service <-> Repository <-> DB.
    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    private String phone;

    private String photoUrl;

    @Column(nullable = false)
    private String role = "user";

    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @Column(nullable = false)
    private boolean notificationsEnabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Column(nullable = false)
    private long authVersion = 0L;

    // Forgot-password support. We never store the OTP itself — only its
    // BCrypt hash (passwordEncoder.encode(), same object we already use
    // for passwordHash) plus an expiry — same "prove it, never store the
    // real secret" principle as passwordHash above. Both are null once
    // there's no pending reset (after a successful reset, or if one was
    // never requested).
    private String resetOtpHash;

    private Instant resetOtpExpiresAt;

    // JPA requires a no-arg constructor — Hibernate builds objects via
    // reflection, not by calling the constructor you'd normally write.
    protected User() {
    }

    public User(String email, String passwordHash, String fullName, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
    }

    // @PrePersist runs automatically, once, right before Hibernate first
    // INSERTs this row — this is where createdAt gets set, so every
    // feature doesn't have to remember to set it manually (same spirit
    // as GlobalExceptionHandler: one place handles a repeated concern).
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // @PreUpdate runs automatically before every UPDATE.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters + setters (Lombok would remove this boilerplate — see
    // Optimization Opportunities table below; not used yet so every line
    // stays visible while you're still learning what each one does).

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getAuthVersion() { return authVersion; }

    public void incrementAuthVersion() { authVersion++; }

    public String getResetOtpHash() { return resetOtpHash; }

    public void setResetOtpHash(String resetOtpHash) { this.resetOtpHash = resetOtpHash; }

    public Instant getResetOtpExpiresAt() { return resetOtpExpiresAt; }

    public void setResetOtpExpiresAt(Instant resetOtpExpiresAt) { this.resetOtpExpiresAt = resetOtpExpiresAt; }
}
