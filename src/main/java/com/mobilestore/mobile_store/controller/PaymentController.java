package com.mobilestore.mobile_store.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mobilestore.mobile_store.dto.request.CreatePaymentIntentRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.PaymentIntentResponseDto;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator.PricingResult;
import com.mobilestore.mobile_store.service.OrderService;
import com.mobilestore.mobile_store.service.PaymentService;
import com.mobilestore.mobile_store.service.PaymentService.PaymentIntentResult;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final OrderPricingCalculator orderPricingCalculator;
    private final PaymentService paymentService;
    private final OrderService orderService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponseDto<PaymentIntentResponseDto>> createIntent(
            @Valid @RequestBody CreatePaymentIntentRequestDto request) {
        PricingResult pricing = orderPricingCalculator.calculate(request.getShippingMethod(), request.getItems(), request.getCouponCode(), request.getEmail(), false);
        PaymentIntentResult intent = paymentService.createPaymentIntent(pricing.total());

        PaymentIntentResponseDto response = PaymentIntentResponseDto.builder()
                .clientSecret(intent.clientSecret())
                .paymentIntentId(intent.id())
                .amount(pricing.total())
                .build();

        return ResponseEntity.ok(ApiResponseDto.success("Payment intent created successfully", response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            log.warn("Stripe webhook signature verification failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        switch (event.getType()) {
            case "charge.refunded" -> handleChargeRefunded(event);
            case "payment_intent.payment_failed" -> log.info("Stripe reported a failed payment intent: {}", event.getId());
            default -> log.debug("Ignoring unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok().build();
    }

    private void handleChargeRefunded(Event event) {
        Optional<StripeObject> dataObject = event.getDataObjectDeserializer().getObject();
        if (dataObject.isPresent() && dataObject.get() instanceof Charge charge) {
            log.info("Processing refund for payment intent {}", charge.getPaymentIntent());
            orderService.cancelOrderByPaymentIntentId(charge.getPaymentIntent());
        }
    }
}
