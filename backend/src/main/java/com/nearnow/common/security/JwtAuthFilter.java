package com.nearnow.common.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;

/**
 * Analogy: this is the door-staff from JwtUtil's comment — it runs on
 * EVERY incoming request, looks for the wristband (JWT in the
 * Authorization header), and if it's genuine, tells Spring Security
 * "this request is from user X, let them through to protected
 * endpoints." If there's no token, or it's invalid, this filter simply
 * does nothing — SecurityConfig (next file) is what actually decides
 * whether a token was REQUIRED for this particular endpoint.
 *
 * This filter has no work to do yet for Auth itself (both Auth endpoints
 * are public — Checkpoint 6). It's built now because every future
 * protected endpoint (Cart, Orders, ...) depends on this existing —
 * building it as part of Auth, since Auth is what introduces JWTs at all.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Expected format: "Authorization: Bearer <token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                Long tokenVersion = jwtUtil.extractAuthVersion(token);
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && tokenVersion != null && tokenVersion == user.getAuthVersion()
                        && role != null && role.equalsIgnoreCase(user.getRole())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        // Always continue the chain — if auth failed/was absent, later
        // logic (SecurityConfig's rules) is what actually blocks the
        // request with a 403, this filter itself never rejects anything.
        filterChain.doFilter(request, response);
    }
}
