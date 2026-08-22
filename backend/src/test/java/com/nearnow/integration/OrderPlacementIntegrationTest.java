package com.nearnow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nearnow.category.Category;
import com.nearnow.category.CategoryRepository;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ONE test in this project's suite that talks to a real, disposable
 * Postgres and a real, disposable Redis instead of a mock. Every other
 * *ServiceTest class (Auth/Order/Admin/Warehouse) mocks its repositories
 * to prove business logic in isolation, in milliseconds. This class
 * proves the opposite thing: that the whole stack — real HTTP request,
 * real Spring Security JWT filter chain, real Hibernate-generated SQL,
 * real Postgres row locks, real Redis-backed rate limiter — actually
 * wires together correctly end to end. Neither kind of test replaces
 * the other; a codebase with only mocked unit tests can still have a
 * broken application.properties or a Hibernate mapping mismatch that no
 * mock would ever catch.
 *
 * Docker must be running locally for this test to execute — Testcontainers
 * pulls (or reuses a cached) postgres/redis image and starts real
 * containers for the lifetime of this test class only, then discards them.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderPlacementIntegrationTest {

    // pgvector/pgvector:pg16, not plain postgres:16 — schema.sql runs
    // `CREATE EXTENSION IF NOT EXISTS vector` on startup (for semantic
    // product search), which a stock Postgres image doesn't ship with.
    // This mirrors docker-compose.yml's own choice of image for exactly
    // the same reason.
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("nearnow_test")
            .withUsername("test")
            .withPassword("test");

    // AuthRateLimitFilter is wired into every request via SecurityConfig
    // and depends on a real RedisTemplate bean at context-startup —
    // there is no "disable rate limiting for tests" switch, and there
    // shouldn't be: the point of this test class is proving the real
    // filter chain works, rate limiter included.
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // These four properties have NO default in application.properties
        // (by design — a real deployment must supply real secrets). A
        // fixed, obviously-fake test value here is fine: this JWT secret
        // and this "Gmail" account never sign or send anything outside
        // this disposable container's lifetime.
        registry.add("jwt.secret", () -> "test-only-secret-key-not-for-production-use-32-chars-minimum");
        registry.add("spring.mail.username", () -> "test@example.com");
        registry.add("spring.mail.password", () -> "test-app-password");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- small helpers shared by every test below ---

    private String registerAndGetToken(String email) throws Exception {
        String body = """
                {"email":"%s","password":"TestPass123","fullName":"Integration Test User"}
                """.formatted(email);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("success").asBoolean()).isTrue();
        return root.path("data").path("token").asText();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // A category+product row is seeded directly through the repositories
    // rather than via HTTP — this test's job is to exercise the checkout
    // path realistically, not to re-prove admin product-creation (that's
    // AdminServiceTest's job). Seeding data this way is a normal,
    // deliberate boundary in integration tests, not a shortcut around them.
    private Product seedActiveProduct(String name, BigDecimal price, int stock) {
        Category category = categoryRepository.save(new Category(name + " Category", null, null, 0));
        Product product = new Product(name, name + " description", category, price, "unit", stock);
        return productRepository.save(product);
    }

    private Long createAddressAndGetId(String token) throws Exception {
        String body = """
                {"label":"Home","fullName":"Integration Test User","phone":"9999999999",
                 "addressLine":"123 Test Street","city":"Delhi","pincode":"110001",
                 "latitude":28.6139,"longitude":77.2090,"isDefault":true}
                """;
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/addresses", new HttpEntity<>(body, authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("data").path("id").asLong();
    }

    // ==================== the main happy-path flow ====================

    @Test
    void fullCheckoutFlow_registerAddToCartAndPlaceOrder_persistsCorrectlyInRealPostgres() throws Exception {
        String token = registerAndGetToken("checkout-flow@nearnow-test.com");
        Product product = seedActiveProduct("Integration Milk", new BigDecimal("50.00"), 20);
        Long addressId = createAddressAndGetId(token);

        // Add 3 units to cart via the real endpoint.
        String addToCartBody = """
                {"productId":%d,"quantity":3}
                """.formatted(product.getId());
        ResponseEntity<String> cartResponse = restTemplate.postForEntity(
                "/api/cart/add", new HttpEntity<>(addToCartBody, authHeaders(token)), String.class);
        assertThat(cartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Place the order.
        String placeOrderBody = """
                {"addressId":%d,"paymentMethod":"COD"}
                """.formatted(addressId);
        ResponseEntity<String> orderResponse = restTemplate.postForEntity(
                "/api/orders", new HttpEntity<>(placeOrderBody, authHeaders(token)), String.class);

        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode orderData = objectMapper.readTree(orderResponse.getBody()).path("data");

        assertThat(orderData.path("status").asText()).isEqualTo("PLACED");
        assertThat(orderData.path("items")).hasSize(1);
        assertThat(orderData.path("items").get(0).path("quantity").asInt()).isEqualTo(3);
        // 3 units x ₹50.00 = ₹150.00 subtotal, before any delivery fee —
        // exact grand-total math (the ₹199 free-delivery threshold) is
        // PricingServiceTest's job; this test only confirms the total
        // reflects the real cart contents, not a hardcoded/stale value.
        assertThat(orderData.path("totalAmount").decimalValue())
                .isGreaterThanOrEqualTo(new BigDecimal("150.00"));

        // Proof this hit a REAL Postgres, not a mock: read the row back
        // directly through the repository, bypassing the HTTP layer.
        Product afterCheckout = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterCheckout.getStock()).isEqualTo(17); // 20 - 3

        // Cart must be empty after checkout (same transaction as order-creation).
        ResponseEntity<String> emptyCartResponse = restTemplate.exchange(
                "/api/cart", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
        JsonNode cartData = objectMapper.readTree(emptyCartResponse.getBody()).path("data");
        assertThat(cartData.path("items")).isEmpty();

        // Order shows up in the caller's own order history.
        ResponseEntity<String> historyResponse = restTemplate.exchange(
                "/api/orders", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
        JsonNode historyData = objectMapper.readTree(historyResponse.getBody()).path("data");
        assertThat(historyData).hasSize(1);
    }

    // ==================== server-side stock enforcement ====================

    @Test
    void placeOrder_requestingMoreThanAvailableStock_isRejectedWith400AndStockUnchanged() throws Exception {
        String token = registerAndGetToken("insufficient-stock@nearnow-test.com");
        Product product = seedActiveProduct("Scarce Item", new BigDecimal("100.00"), 2); // only 2 in stock
        Long addressId = createAddressAndGetId(token);

        String addToCartBody = """
                {"productId":%d,"quantity":5}
                """.formatted(product.getId()); // asking for more than exists
        restTemplate.postForEntity(
                "/api/cart/add", new HttpEntity<>(addToCartBody, authHeaders(token)), String.class);

        String placeOrderBody = """
                {"addressId":%d,"paymentMethod":"COD"}
                """.formatted(addressId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders", new HttpEntity<>(placeOrderBody, authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("message").asText()).contains("available");

        // Nothing should have been deducted from a rejected checkout.
        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getStock()).isEqualTo(2);
    }

    // ==================== cross-cutting: real security filter chain ====================

    @Test
    void protectedEndpoint_withNoToken_isRejectedByRealJwtFilterChain() {
        // No Authorization header at all — this must be blocked by the
        // real JwtAuthFilter + SecurityConfig chain running in a real
        // servlet container, not a mocked SecurityContext like a @WebMvcTest
        // slice test would use.
        ResponseEntity<String> response = restTemplate.getForEntity("/api/orders", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void productBrowsing_withNoToken_isPubliclyReadable() {
        // /api/products/** is explicitly permitAll in SecurityConfig —
        // confirms the real filter chain distinguishes public browsing
        // from the authenticated-only endpoints above, not just "auth
        // header present or not" as a blanket rule.
        ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
