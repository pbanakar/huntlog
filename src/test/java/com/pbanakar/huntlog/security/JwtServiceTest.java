package com.pbanakar.huntlog.security;

import com.pbanakar.huntlog.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // A test secret of at least 256 bits (32 bytes)
        String secret = "test-secret-key-that-is-at-least-256-bits-long-for-testing-only";
        long expirationMs = 3600000; // 1 hour
        jwtService = new JwtService(secret, expirationMs);

        testUser = new User();
        testUser.setId(42L);
        testUser.setEmail("developer@example.com");
        testUser.setName("Prajwal");
    }

    @Test
    @DisplayName("Generate token contains valid email and userId claims")
    void generateToken_extractsClaimsCorrectly() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("developer@example.com", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Invalid token fails validation")
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    @DisplayName("Expired token fails validation")
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtService shortLivedJwt = new JwtService("test-secret-key-that-is-at-least-256-bits-long-for-testing-only", 1); // 1ms
        String token = shortLivedJwt.generateToken(testUser);
        Thread.sleep(10);

        assertFalse(shortLivedJwt.validateToken(token));
    }
}
