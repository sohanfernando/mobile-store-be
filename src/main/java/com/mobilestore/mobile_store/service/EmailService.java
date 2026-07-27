package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.messaging.OrderCreatedEvent;

public interface EmailService {
    void sendOrderConfirmationEmail(OrderCreatedEvent event);

    // These four are triggered synchronously from an admin status-change action, so unlike the
    // others they return whether the send actually succeeded - the caller surfaces that to the admin.
    boolean sendPaymentCompletedEmail(Order order);
    boolean sendOrderShippedEmail(Order order);
    boolean sendOrderDeliveredEmail(Order order);
    boolean sendOrderCancelledEmail(Order order);

    // Fire-and-forget variants for flows (checkout completion, payment-intent cancellation) that
    // must not block their request thread on the outcome the way the admin flow does.
    void sendPaymentCompletedEmailAsync(Order order);
    void sendOrderCancelledEmailAsync(Order order);

    void sendOtpEmail(String toEmail, String otpCode);
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
