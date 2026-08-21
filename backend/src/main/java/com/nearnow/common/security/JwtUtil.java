package com.nearnow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final Key signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs,
                   @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(Long userId, String email, String role, long authVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("authVersion", authVersion)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // A long-lived, narrow-purpose token: it proves "this device was
    // recently logged in" and can be exchanged for a new short-lived
    // access token, but it deliberately does NOT carry a `role` claim
    // — the refresh endpoint always re-reads the CURRENT role from the
    // database rather than trusting a role that might be stale by the
    // time a 30-day-old refresh token gets used.
    public String generateRefreshToken(Long userId, String email, long authVersion) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("authVersion", authVersion)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

        public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.out.println("JWT validation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }
    
    public String extractEmail(String token) { return extractClaims(token).getSubject(); }
    public Long extractUserId(String token) { return extractClaims(token).get("userId", Long.class); }
    public String extractRole(String token) { return extractClaims(token).get("role", String.class); }
    public Long extractAuthVersion(String token) { return extractClaims(token).get("authVersion", Long.class); }
    public String extractTokenType(String token) { return extractClaims(token).get("type", String.class); }

    // Access tokens minted before this field existed have no "type"
    // claim at all — treated as "access" so already-issued tokens
    // don't suddenly break on the next deploy.
    public boolean isRefreshToken(String token) { return "refresh".equals(extractTokenType(token)); }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }
}
