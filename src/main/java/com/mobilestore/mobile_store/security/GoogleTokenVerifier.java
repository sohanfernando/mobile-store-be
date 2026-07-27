package com.mobilestore.mobile_store.security;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mobilestore.mobile_store.exception.GoogleAuthException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Google Sign-In ID tokens server-side (signature, issuer, audience, expiry)
 * via Google's official client library, rather than trusting anything the browser sends.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    @Value("${app.google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        if (clientId != null && !clientId.isBlank()) {
            verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
        }
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        if (verifier == null) {
            throw new GoogleAuthException("Google Sign-In is not configured on this server");
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new GoogleAuthException("Invalid or expired Google sign-in token");
            }
            return idToken.getPayload();
        } catch (GoogleAuthException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID token verification failed", e);
            throw new GoogleAuthException("Failed to verify Google sign-in token");
        }
    }
}
