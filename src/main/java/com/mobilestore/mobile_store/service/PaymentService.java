package com.mobilestore.mobile_store.service;

import java.math.BigDecimal;

public interface PaymentService {

    record PaymentIntentResult(String id, String clientSecret, String status, Long amount) {
    }

    PaymentIntentResult createPaymentIntent(BigDecimal amount);

    PaymentIntentResult retrievePaymentIntent(String paymentIntentId);
}
