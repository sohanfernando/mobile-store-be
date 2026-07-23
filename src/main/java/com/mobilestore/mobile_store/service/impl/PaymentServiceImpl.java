package com.mobilestore.mobile_store.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mobilestore.mobile_store.exception.PaymentException;
import com.mobilestore.mobile_store.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.currency}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(BigDecimal amount) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toSmallestCurrencyUnit(amount))
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            return toResult(PaymentIntent.create(params));
        } catch (StripeException ex) {
            throw new PaymentException("Failed to create payment intent: " + ex.getMessage());
        }
    }

    @Override
    public PaymentIntentResult retrievePaymentIntent(String paymentIntentId) {
        try {
            return toResult(PaymentIntent.retrieve(paymentIntentId));
        } catch (StripeException ex) {
            throw new PaymentException("Failed to retrieve payment intent: " + ex.getMessage());
        }
    }

    public static long toSmallestCurrencyUnit(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private PaymentIntentResult toResult(PaymentIntent intent) {
        return new PaymentIntentResult(intent.getId(), intent.getClientSecret(), intent.getStatus(), intent.getAmount());
    }
}
