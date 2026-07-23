package com.mobilestore.mobile_store.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.exception.TooManyLoginAttemptsException;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(AtomicInteger count, Instant startedAt) {
    }

    private final ConcurrentHashMap<String, Window> attemptsByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Window window = attemptsByKey.get(key);
        if (window != null && !isExpired(window) && window.count().get() >= MAX_ATTEMPTS) {
            long retryAfterSeconds = WINDOW.minus(Duration.between(window.startedAt(), Instant.now())).getSeconds();
            throw new TooManyLoginAttemptsException(Math.max(retryAfterSeconds, 1));
        }
    }

    public void recordFailure(String key) {
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Window(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }

    private boolean isExpired(Window window) {
        return Duration.between(window.startedAt(), Instant.now()).compareTo(WINDOW) >= 0;
    }
}
