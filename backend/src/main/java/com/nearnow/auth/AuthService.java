package com.nearnow.auth;

import com.nearnow.common.exception.DuplicateResourceException;
import com.nearnow.common.exception.InvalidCredentialsException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.common.security.JwtUtil;
import com.nearnow.notification.NotificationService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * This is the Java-equivalent of firebase_auth_service.dart +
 * auth_repository.dart combined — but where Firebase used to do
 * password-handling invisibly on Google's servers, this class now does
 * it explicitly: hashing, verifying, and issuing our own tokens.
 */
@Service
public class AuthService {

    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.notificationService = notificationService;
    }

    // @Transactional groups the uniqueness-check and save into one DB
    // transaction, but it does NOT by itself prevent two READ_COMMITTED
    // transactions from both passing existsByEmail(). The database UNIQUE
    // constraint on users.email is the final race-safe guard; the global
    // DataIntegrityViolationException handler converts a losing insert
    // into a clean 409 instead of an opaque 500.
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // BCrypt: a one-way hash — we can check "does this password
        // match?" later, but we can NEVER reverse the hash back into
        // the original password. This is why the field is called
        // passwordHash, not password — we deliberately never have the
        // real password in our database, only proof we could verify one.
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                hashedPassword,
                request.getFullName(),
                "" // phone — not collected at registration in the current Flutter flow
        );

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole(), saved.getAuthVersion());
        String refreshToken = jwtUtil.generateRefreshToken(saved.getId(), saved.getEmail(), saved.getAuthVersion());

        return new AuthResponseDTO(token, refreshToken, saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                // Deliberately the SAME exception+message as a wrong
                // password below — see InvalidCredentialsException's
                // own comment for why.
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // passwordEncoder.matches() re-hashes the incoming plain-text
        // password with the same algorithm and compares hashes — this
        // is the "check without ever reversing" step BCrypt enables.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getAuthVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getAuthVersion());

        return new AuthResponseDTO(token, refreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    // Exchanges a still-valid refresh token for a brand-new access
    // token + a brand-new refresh token ("rotation" — every refresh
    // issues a fresh pair rather than reusing the same refresh token
    // forever, which limits how long a stolen refresh token stays useful).
    public AuthResponseDTO refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        Long tokenAuthVersion = jwtUtil.extractAuthVersion(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        // authVersion mismatch means the user logged out (or changed
        // password) SINCE this refresh token was issued — reject it
        // even though the JWT signature itself is still technically valid.
        if (tokenAuthVersion == null || tokenAuthVersion != user.getAuthVersion()) {
            throw new InvalidCredentialsException("Session expired, please log in again");
        }

        String newToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getAuthVersion());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getAuthVersion());

        return new AuthResponseDTO(newToken, newRefreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    // "Logout everywhere": bumping authVersion makes every access token
    // AND every refresh token issued before this moment fail the
    // authVersion check above / in JwtAuthFilter — without needing a
    // server-side token blacklist. This is also what SHOULD run on a
    // password-change flow later, for the same reason.
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists"));
        user.incrementAuthVersion();
        userRepository.save(user);
    }

    // Step 1 of the reset flow: generate an OTP, store only its hash +
    // expiry, "send" it (NotificationService logs it in dev). This
    // method deliberately does the SAME thing whether or not the email
    // exists — no early-return, no exception, no different response —
    // for the identical enumeration-defense reason InvalidCredentialsException's
    // own comment gives for login. Silent no-op on a real DB miss is
    // indistinguishable from the real "we sent it" path to the caller.
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
            user.setResetOtpHash(passwordEncoder.encode(otp));
            user.setResetOtpExpiresAt(Instant.now().plusSeconds(OTP_VALIDITY_MINUTES * 60L));
            userRepository.save(user);
            notificationService.sendPasswordResetOtp(user.getEmail(), otp);
        });
    }

    // Step 2: spend the OTP. Unlike forgotPassword above, THIS step is
    // allowed to say "wrong" — the caller already proved they saw the
    // OTP (i.e. they have inbox/log access), so there's no enumeration
    // risk left to protect here, only normal invalid-input handling.
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset code"));

        if (user.getResetOtpHash() == null || user.getResetOtpExpiresAt() == null
                || user.getResetOtpExpiresAt().isBefore(Instant.now())
                || !passwordEncoder.matches(otp, user.getResetOtpHash())) {
            throw new InvalidCredentialsException("Invalid or expired reset code");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // One-time use: clear the OTP so it can't be replayed.
        user.setResetOtpHash(null);
        user.setResetOtpExpiresAt(null);
        // Same "logout everywhere" step logout() does — a password reset
        // is exactly the kind of event that should invalidate every
        // token issued before it, including on a device that stole the
        // old password but not this new one.
        user.incrementAuthVersion();

        userRepository.save(user);
    }

    // Called by GET /api/auth/me. `email` comes from the JWT that
    // JwtAuthFilter already validated — by the time this method runs,
    // Spring Security has already confirmed the token is genuine, so we
    // don't re-check the password here, only look the profile up.
    // ResourceNotFoundException here would mean the JWT was valid but the
    // user row was deleted after the token was issued — an edge case,
    // not the normal path.
    public UserResponseDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists"));

        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getPhotoUrl(),
                user.getRole(),
                user.isEmailVerified(),
                user.isNotificationsEnabled()
        );
    }

    @Transactional
    public UserResponseDTO updateProfile(String email, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists"));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }

        User saved = userRepository.save(user);

        return new UserResponseDTO(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getPhone(),
                saved.getPhotoUrl(),
                saved.getRole(),
                saved.isEmailVerified(),
                saved.isNotificationsEnabled()
        );
    }    @Transactional
    public UserResponseDTO updateNotificationPreference(String email, boolean enabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists"));

        user.setNotificationsEnabled(enabled);

        User saved = userRepository.save(user);

        return new UserResponseDTO(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getPhone(),
                saved.getPhotoUrl(),
                saved.getRole(),
                saved.isEmailVerified(),
                saved.isNotificationsEnabled()
        );
    }}
