package com.mobilestore.mobile_store.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.mobilestore.mobile_store.exception.TooManyLoginAttemptsException;

class LoginRateLimiterTest {

    @Test
    void allowsUpToFiveFailuresThenBlocks() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "1.2.3.4";

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.checkAllowed(key));
            limiter.recordFailure(key);
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed(key));
    }

    @Test
    void successResetsFailureCount() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "5.6.7.8";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        limiter.recordSuccess(key);

        assertDoesNotThrow(() -> limiter.checkAllowed(key));
    }

    @Test
    void differentKeysAreTrackedIndependently() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("attacker-ip");
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("attacker-ip"));
        assertDoesNotThrow(() -> limiter.checkAllowed("innocent-ip"));
    }
}
