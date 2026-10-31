package com.pbanakar.huntlog.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {

    @Test
    void blocksAttemptsAfterConfiguredLimit() {
        AuthRateLimiter limiter = new AuthRateLimiter(2, 60);

        assertTrue(limiter.isAllowed("login:127.0.0.1"));
        assertTrue(limiter.isAllowed("login:127.0.0.1"));
        assertFalse(limiter.isAllowed("login:127.0.0.1"));
        assertTrue(limiter.isAllowed("login:127.0.0.2"));
    }
}
