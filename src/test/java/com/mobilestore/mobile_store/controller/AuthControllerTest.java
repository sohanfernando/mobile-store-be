package com.mobilestore.mobile_store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.mobilestore.mobile_store.entity.Customer;
import com.mobilestore.mobile_store.entity.EmailOtp;
import com.mobilestore.mobile_store.exception.GoogleAuthException;
import com.mobilestore.mobile_store.exception.InvalidOtpException;
import com.mobilestore.mobile_store.exception.TooManyLoginAttemptsException;
import com.mobilestore.mobile_store.repository.AdminRepository;
import com.mobilestore.mobile_store.repository.CustomerRepository;
import com.mobilestore.mobile_store.repository.EmailOtpRepository;
import com.mobilestore.mobile_store.repository.RevokedTokenRepository;
import com.mobilestore.mobile_store.security.GoogleTokenVerifier;
import com.mobilestore.mobile_store.security.JwtService;
import com.mobilestore.mobile_store.security.LoginRateLimiter;
import com.mobilestore.mobile_store.service.EmailService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private EmailOtpRepository emailOtpRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthController controller;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        controller = new AuthController(adminRepository, customerRepository, emailOtpRepository,
                passwordEncoder, jwtService, new LoginRateLimiter(), revokedTokenRepository,
                emailService, googleTokenVerifier);
    }

    @Test
    void sendOtpNeverReturnsTheCodeInTheResponse() {
        var response = controller.sendOtp(Map.of("email", "buyer@example.com"), new MockHttpServletRequest());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void sendOtpEmailsTheCodeAndStoresOnlyAHash() {
        controller.sendOtp(Map.of("email", "buyer@example.com"), new MockHttpServletRequest());

        ArgumentCaptor<EmailOtp> saved = ArgumentCaptor.forClass(EmailOtp.class);
        verify(emailOtpRepository).save(saved.capture());
        verify(emailService).sendOtpEmail(org.mockito.ArgumentMatchers.eq("buyer@example.com"), anyString());

        assertThat(saved.getValue().getOtpHash()).doesNotContain("-"); // not a plain 6-digit code
        assertThat(saved.getValue().getOtpHash().length()).isGreaterThan(6);
    }

    @Test
    void repeatedOtpRequestsAreRateLimited() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        for (int i = 0; i < 5; i++) {
            controller.sendOtp(Map.of("email", "spam@example.com"), request);
        }

        assertThrows(TooManyLoginAttemptsException.class,
                () -> controller.sendOtp(Map.of("email", "spam@example.com"), request));
    }

    @Test
    void verifyOtpSucceedsWithCorrectCodeAndIssuesCustomerToken() {
        EmailOtp record = EmailOtp.builder()
                .email("buyer@example.com")
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .consumed(false)
                .build();
        when(emailOtpRepository.findFirstByEmailIgnoreCaseAndConsumedFalseOrderByCreatedAtDesc("buyer@example.com"))
                .thenReturn(Optional.of(record));
        when(customerRepository.findByEmail("buyer@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("buyer@example.com", "CUSTOMER")).thenReturn("jwt-token");

        var response = controller.verifyOtp(Map.of("email", "buyer@example.com", "otp", "123456"));

        assertThat(response.getBody().getData().get("token")).isEqualTo("jwt-token");
        assertThat(record.isConsumed()).isTrue();
    }

    @Test
    void verifyOtpRejectsWrongCode() {
        EmailOtp record = EmailOtp.builder()
                .email("buyer@example.com")
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(emailOtpRepository.findFirstByEmailIgnoreCaseAndConsumedFalseOrderByCreatedAtDesc("buyer@example.com"))
                .thenReturn(Optional.of(record));

        assertThrows(InvalidOtpException.class,
                () -> controller.verifyOtp(Map.of("email", "buyer@example.com", "otp", "999999")));
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void verifyOtpRejectsExpiredCode() {
        EmailOtp record = EmailOtp.builder()
                .email("buyer@example.com")
                .otpHash(passwordEncoder.encode("123456"))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(emailOtpRepository.findFirstByEmailIgnoreCaseAndConsumedFalseOrderByCreatedAtDesc("buyer@example.com"))
                .thenReturn(Optional.of(record));

        assertThrows(InvalidOtpException.class,
                () -> controller.verifyOtp(Map.of("email", "buyer@example.com", "otp", "123456")));
    }

    @Test
    void verifyOtpRejectsWhenNoOtpWasEverSent() {
        when(emailOtpRepository.findFirstByEmailIgnoreCaseAndConsumedFalseOrderByCreatedAtDesc("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class,
                () -> controller.verifyOtp(Map.of("email", "ghost@example.com", "otp", "123456")));
    }

    @Test
    void googleLoginCreatesNewCustomerAndIssuesToken() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("googler@example.com");
        payload.setEmailVerified(true);
        payload.setSubject("google-sub-123");
        payload.set("name", "Googler Person");

        when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
        when(customerRepository.findByEmail("googler@example.com")).thenReturn(Optional.empty());
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("googler@example.com", "CUSTOMER")).thenReturn("jwt-token");

        var response = controller.googleLogin(Map.of("credential", "id-token"));

        assertThat(response.getBody().getData().get("token")).isEqualTo("jwt-token");
        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getGoogleId()).isEqualTo("google-sub-123");
        assertThat(saved.getValue().getName()).isEqualTo("Googler Person");
    }

    @Test
    void googleLoginRejectsUnverifiedEmail() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("unverified@example.com");
        payload.setEmailVerified(false);

        when(googleTokenVerifier.verify("id-token")).thenReturn(payload);

        assertThrows(GoogleAuthException.class, () -> controller.googleLogin(Map.of("credential", "id-token")));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void googleLoginLinksExistingCustomerByEmailWithoutOverwritingName() {
        Customer existing = Customer.builder().email("existing@example.com").name("Original Name").build();
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("existing@example.com");
        payload.setEmailVerified(true);
        payload.setSubject("google-sub-456");
        payload.set("name", "New Google Name");

        when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
        when(customerRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("existing@example.com", "CUSTOMER")).thenReturn("jwt-token");

        controller.googleLogin(Map.of("credential", "id-token"));

        assertThat(existing.getGoogleId()).isEqualTo("google-sub-456");
        assertThat(existing.getName()).isEqualTo("Original Name");
    }
}
