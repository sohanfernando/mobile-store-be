package com.mobilestore.mobile_store.security;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.repository.EmailOtpRepository;
import com.mobilestore.mobile_store.repository.RevokedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokedTokenCleanupTask {

    private final RevokedTokenRepository revokedTokenRepository;
    private final EmailOtpRepository emailOtpRepository;

    // Revoked entries are only useful until the token itself would have expired anyway.
    // Expired OTPs are cleaned up alongside them for the same reason.
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredEntries() {
        int deletedTokens = revokedTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deletedTokens > 0) {
            log.info("Purged {} expired revoked-token entries", deletedTokens);
        }

        int deletedOtps = emailOtpRepository.deleteAllExpiredBefore(LocalDateTime.now());
        if (deletedOtps > 0) {
            log.info("Purged {} expired email OTP entries", deletedOtps);
        }
    }
}
