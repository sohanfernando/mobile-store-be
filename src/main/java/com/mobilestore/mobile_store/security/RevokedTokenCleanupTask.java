package com.mobilestore.mobile_store.security;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.repository.RevokedTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevokedTokenCleanupTask {

    private final RevokedTokenRepository revokedTokenRepository;

    // Revoked entries are only useful until the token itself would have expired anyway.
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredEntries() {
        int deleted = revokedTokenRepository.deleteAllExpiredBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired revoked-token entries", deleted);
        }
    }
}
