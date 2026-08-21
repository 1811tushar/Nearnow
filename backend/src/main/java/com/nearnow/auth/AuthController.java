package com.nearnow.auth;

import com.nearnow.common.config.SecurityConfig;
import com.nearnow.common.dto.ApiResponse;
import com.nearnow.common.security.JwtAuthFilter;
import com.nearnow.common.security.JwtUtil;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Only 3 endpoints for now, per Checkpoint 5's locked-in scope —
 * sendPasswordResetEmail/deleteCurrentUser from the old
 * auth_repository.dart are deferred, core flow first.
 * (Profile editing added afterward once the Flutter save-changes
 * screen needed a real endpoint to call.)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // @Valid triggers the RegisterRequestDTO's validation annotations
    // (@NotBlank, @Email, @Size) BEFORE this method body runs — a
    // malformed request never even reaches register() below.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED) // 201 — a new resource (the user) was created
                .body(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.success(response, "Login successful")); // 200
    }

    // Deliberately NOT in SecurityConfig's authenticated-by-default
    // bucket — the caller here has no valid access token (it likely
    // just expired, that's the whole reason they're calling this), so
    // this must be reachable with only a refresh token in the body.
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        AuthResponseDTO response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    // Deliberately ALWAYS returns the same generic success message,
    // whether or not that email is actually registered — see
    // AuthService.forgotPassword()'s comment. Permitted without a JWT
    // (SecurityConfig) — the whole point is the caller is locked out.
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null,
                "If that email is registered, a reset code has been sent"));
    }

    // Also permitAll — same reasoning as /forgot-password above.
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successful, please log in again"));
    }

    // The opposite of refresh: requires a currently-valid access token
    // (so we know WHO is logging out), then invalidates every token —
    // access and refresh — issued to that user up to this point.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    // NOT in SecurityConfig's permitAll list -> requires a valid JWT.
    // `Authentication` here is the object JwtAuthFilter populated
    // (Phase 2 Security step) — `.getName()` returns the email we set
    // as the token's subject in JwtUtil.generateToken(). Spring injects
    // this parameter automatically; we never construct it ourselves.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(Authentication authentication) {
        UserResponseDTO response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Also NOT in SecurityConfig's permitAll list -> requires a valid
    // JWT, same reasoning as /me above. Reuses the same
    // authentication.getName() pattern to find out WHO is updating
    // their profile, rather than trusting a userId sent in the body.
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        UserResponseDTO response = authService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }
    // PATCH, not PUT — this endpoint only ever changes ONE field
    // (notificationsEnabled), not the whole profile resource. Same
    // authentication.getName() ownership pattern as /profile above.
    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateNotifications(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationsRequestDTO request) {
        UserResponseDTO response = authService.updateNotificationPreference(
                authentication.getName(), request.getEnabled());
        return ResponseEntity.ok(ApiResponse.success(response, "Notification preference updated"));
    }}