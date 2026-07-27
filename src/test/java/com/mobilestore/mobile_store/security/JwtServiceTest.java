package com.mobilestore.mobile_store.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-only-secret-key-must-be-at-least-32-chars-long");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);
    }

    @Test
    void generatedTokenRoundTripsToOriginalEmail() {
        String token = jwtService.generateToken("admin@example.com", "ADMIN");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@example.com");
    }

    @Test
    void generatedTokenCarriesTheGivenRole() {
        String adminToken = jwtService.generateToken("admin@example.com", "ADMIN");
        String customerToken = jwtService.generateToken("customer@example.com", "CUSTOMER");

        assertThat(jwtService.extractRole(adminToken)).isEqualTo("ADMIN");
        assertThat(jwtService.extractRole(customerToken)).isEqualTo("CUSTOMER");
    }

    @Test
    void generatedTokenHasAUniqueJti() {
        String tokenA = jwtService.generateToken("admin@example.com", "ADMIN");
        String tokenB = jwtService.generateToken("admin@example.com", "ADMIN");

        assertThat(jwtService.extractJti(tokenA)).isNotBlank();
        assertThat(jwtService.extractJti(tokenA)).isNotEqualTo(jwtService.extractJti(tokenB));
    }

    @Test
    void garbageTokenIsInvalid() {
        assertThat(jwtService.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsInvalid() {
        String token = jwtService.generateToken("admin@example.com", "ADMIN");

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret", "a-completely-different-secret-key-that-is-also-long-enough");
        ReflectionTestUtils.setField(otherService, "expirationMs", 86_400_000L);

        assertThat(otherService.isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String token = jwtService.generateToken("admin@example.com", "ADMIN");

        assertThat(jwtService.isValid(token)).isFalse();
    }
}
