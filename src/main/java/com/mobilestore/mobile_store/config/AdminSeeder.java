package com.mobilestore.mobile_store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.entity.Admin;
import com.mobilestore.mobile_store.repository.AdminRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Seeds a single admin account on first run only (skipped once any admin row exists).
// Override app.admin.seed-email / app.admin.seed-password via env vars before first
// startup in any real environment instead of relying on the defaults below.
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-email}")
    private String seedEmail;

    @Value("${app.admin.seed-password}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = Admin.builder()
                    .email(seedEmail)
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .build();
            adminRepository.save(admin);
            log.info("Seeded initial admin account: {}", seedEmail);
        }
    }
}
