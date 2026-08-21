package com.nearnow.common.config;

import com.nearnow.common.security.JwtAuthFilter;
import com.nearnow.common.security.AuthRateLimitFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

/**
 * This is Checkpoint 6 made real: the single place that decides which
 * endpoints are public and which require a valid JWT. Every future
 * feature's Controller gets its security decision added HERE, not
 * scattered across individual Controllers — same "one place for a
 * repeated concern" discipline as GlobalExceptionHandler.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthRateLimitFilter authRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authRateLimitFilter = authRateLimitFilter;
    }

    // Exposed as a Spring Bean so AuthService (and any future service)
    // can have it injected via constructor, rather than each service
    // creating its own `new BCryptPasswordEncoder()`.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Flutter (running on an Android emulator, iOS simulator, or a
    // browser tab for web) is a DIFFERENT origin than this backend —
    // without this bean, the browser/webview's CORS-policy silently
    // blocks every request before it even reaches a Controller. This
    // is a dev-permissive config (allowedOrigins "*") — fine for local
    // development against an emulator, would need tightening to a
    // specific domain list before any real production deployment.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Wire the CORS bean above into the security filter chain —
                // defining the bean alone does nothing until Spring Security
                // is told to actually apply it here.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF protection exists to stop browser-cookie-based attacks.
                // We're a stateless REST API using JWTs, not cookies — CSRF
                // doesn't apply here, so we disable it (standard for JWT APIs).
                .csrf(csrf -> csrf.disable())

                // Tell Spring Security: never create or use an HTTP session.
                // Every request must prove who it is via the JWT alone —
                // this is what "stateless" means in Section 5's Auth table.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Checkpoint 6's decision, made real: ONLY these two
                        // exact paths skip authentication. Deliberately NOT
                        // using a "/api/auth/**" wildcard here — that would
                        // have accidentally also made /api/auth/me public,
                        // silently defeating the whole point of that endpoint.
                        // Explicit path-by-path is safer than a wildcard
                        // whenever a "sometimes public" prefix exists.
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        // NEW (Phase 4): browsing products/categories needs
                        // no login — same as walking into Blinkit's app
                        // before signing in. Checkout (Phase 8, Orders)
                        // will be the actual point where a JWT becomes
                        // required, not browsing.
                        .requestMatchers("/api/products/**", "/api/categories/**").permitAll()
                        // Reviews: only GET is public (browsing) — POST
                        // stays out of this list, defaulting to
                        // authenticated below. Explicit HTTP-method-scoped
                        // matcher, not a blanket "/api/reviews/**" wildcard
                        // — same lesson as the Auth wildcard-bug caught earlier.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/**").permitAll()
                        // Admin: role-based, not just "must be logged in" —
                        // first use of hasRole() in this project. Matches
                        // "ROLE_" + role JwtAuthFilter already grants
                        // (built in Phase 2, unused until Phase 10).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE_MANAGER")
                        .requestMatchers("/api/vendor/**").hasRole("VENDOR")
                        .requestMatchers("/api/rider/**").hasRole("RIDER")
                        // Prometheus scrapes this on a schedule, with no
                        // user/JWT — must be reachable without auth.
                        // show-details=when-authorized (set above) still
                        // limits what an unauthenticated caller can see.
                        .requestMatchers("/actuator/**").permitAll()
                        // Everything else — including /api/auth/me — defaults
                        // to "must be authenticated."
                        .anyRequest().authenticated()
                )

                // Insert our JWT filter to run BEFORE Spring's default
                // username/password filter — we're replacing that mechanism
                // entirely with JWT-based auth.
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}