package com.mobilestore.mobile_store.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.mobilestore.mobile_store.dto.request.LoginRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.entity.Admin;
import com.mobilestore.mobile_store.entity.Customer;
import com.mobilestore.mobile_store.entity.EmailOtp;
import com.mobilestore.mobile_store.entity.RevokedToken;
import com.mobilestore.mobile_store.exception.GoogleAuthException;
import com.mobilestore.mobile_store.exception.InvalidOtpException;
import com.mobilestore.mobile_store.repository.AdminRepository;
import com.mobilestore.mobile_store.repository.CustomerRepository;
import com.mobilestore.mobile_store.repository.EmailOtpRepository;
import com.mobilestore.mobile_store.repository.RevokedTokenRepository;
import com.mobilestore.mobile_store.security.GoogleTokenVerifier;
import com.mobilestore.mobile_store.security.JwtService;
import com.mobilestore.mobile_store.security.LoginRateLimiter;
import com.mobilestore.mobile_store.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int OTP_VALIDITY_MINUTES = 5;

    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final RevokedTokenRepository revokedTokenRepository;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> login(
            @Valid @RequestBody LoginRequestDto request, HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        loginRateLimiter.checkAllowed(clientIp);

        Admin admin = adminRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDto.error("Invalid email or password"));
        }

        loginRateLimiter.recordSuccess(clientIp);
        String token = jwtService.generateToken(admin.getEmail(), "ADMIN");
        Map<String, String> data = Map.of(
                "token", token,
                "email", admin.getEmail(),
                "role", "ADMIN"
        );
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", data));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponseDto<Void>> sendOtp(@RequestBody Map<String, String> body,
            HttpServletRequest servletRequest) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponseDto.error("Email address is required"));
        }
        email = email.trim();

        // Same limiter used for admin login, keyed separately - stops an email address (or IP)
        // from being spammed with OTP emails.
        String rateLimitKey = "otp:" + extractClientIp(servletRequest) + ":" + email.toLowerCase();
        loginRateLimiter.checkAllowed(rateLimitKey);
        loginRateLimiter.recordFailure(rateLimitKey);

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        emailOtpRepository.save(EmailOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otpCode))
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                .build());

        // The code only ever leaves the server via email - never in the API response.
        emailService.sendOtpEmail(email, otpCode);

        return ResponseEntity.ok(ApiResponseDto.success("OTP sent successfully", null));
    }

    @PostMapping("/verify-otp")
    @Transactional
    public ResponseEntity<ApiResponseDto<Map<String, String>>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            throw new InvalidOtpException("Email and verification code are required");
        }
        String normalizedEmail = email.trim();

        EmailOtp record = emailOtpRepository
                .findFirstByEmailIgnoreCaseAndConsumedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new InvalidOtpException(
                        "No active verification code found for this email. Please request a new one."));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("Verification code has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(otp.trim(), record.getOtpHash())) {
            throw new InvalidOtpException("Invalid verification code");
        }

        record.setConsumed(true);
        emailOtpRepository.save(record);

        Customer customer = customerRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> customerRepository.save(Customer.builder().email(normalizedEmail).build()));

        String token = jwtService.generateToken(customer.getEmail(), "CUSTOMER");
        Map<String, String> data = Map.of("token", token, "email", customer.getEmail());
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", data));
    }

    @PostMapping("/customer/google")
    @Transactional
    public ResponseEntity<ApiResponseDto<Map<String, String>>> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            throw new GoogleAuthException("Missing Google credential");
        }

        GoogleIdToken.Payload payload = googleTokenVerifier.verify(credential);
        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified == null || !emailVerified) {
            throw new GoogleAuthException("This Google account's email is not verified");
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleId = payload.getSubject();

        Customer customer = customerRepository.findByEmail(email)
                .orElseGet(() -> Customer.builder().email(email).name(name).build());
        if (customer.getGoogleId() == null) {
            customer.setGoogleId(googleId);
        }
        if ((customer.getName() == null || customer.getName().isBlank()) && name != null) {
            customer.setName(name);
        }
        customer = customerRepository.save(customer);

        String token = jwtService.generateToken(customer.getEmail(), "CUSTOMER");
        Map<String, String> data = Map.of("token", token, "email", customer.getEmail());
        return ResponseEntity.ok(ApiResponseDto.success("Login successful", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(HttpServletRequest servletRequest) {
        String header = servletRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                revokedTokenRepository.save(RevokedToken.builder()
                        .jti(jwtService.extractJti(token))
                        .expiresAt(jwtService.extractExpiry(token).toInstant())
                        .build());
            }
        }
        return ResponseEntity.ok(ApiResponseDto.success("Logged out successfully", null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
