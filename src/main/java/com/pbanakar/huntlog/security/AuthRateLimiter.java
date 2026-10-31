package com.pbanakar.huntlog.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small, bounded per-instance limiter for unauthenticated auth endpoints.
 * A shared store should replace this when running multiple application instances.
 */
@Component
public class AuthRateLimiter {

    private final Map<String, AttemptWindow> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public AuthRateLimiter(@Value("${app.security.auth-rate-limit.max-attempts:10}") int maxAttempts,
                           @Value("${app.security.auth-rate-limit.window-seconds:60}") long windowSeconds) {
        if (maxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Auth rate-limit settings must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean isAllowed(String key) {
        Instant now = Instant.now();
        AttemptWindow windowState = windows.compute(key, (ignored, current) -> {
            if (current == null || current.startedAt.plus(window).isBefore(now)) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(current.startedAt, current.attempts + 1);
        });

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().startedAt.plus(window).isBefore(now));
        }
        return windowState.attempts <= maxAttempts;
    }

    private record AttemptWindow(Instant startedAt, int attempts) {
    }
}
