package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.messaging.OrderCreatedEvent;

public interface EmailService {
    void sendOrderConfirmationEmail(OrderCreatedEvent event);
    void sendOtpEmail(String toEmail, String otpCode);
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
