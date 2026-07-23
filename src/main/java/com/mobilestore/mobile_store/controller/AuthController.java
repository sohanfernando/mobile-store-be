package com.mobilestore.mobile_store.controller;

import com.mobilestore.mobile_store.dto.request.LoginRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.entity.Admin;
import com.mobilestore.mobile_store.entity.RevokedToken;
import com.mobilestore.mobile_store.repository.AdminRepository;
import com.mobilestore.mobile_store.repository.RevokedTokenRepository;
import com.mobilestore.mobile_store.security.JwtService;
import com.mobilestore.mobile_store.security.LoginRateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final RevokedTokenRepository revokedTokenRepository;

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
        String token = jwtService.generateToken(admin.getEmail());
        Map<String, String> data = Map.of(
                "token", token,
                "email", admin.getEmail(),
                "role", "ADMIN"
        );
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
