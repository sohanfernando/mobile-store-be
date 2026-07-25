package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.messaging.OrderCreatedEvent;

public interface EmailService {
    void sendOrderConfirmationEmail(OrderCreatedEvent event);
    void sendPaymentCompletedEmail(Order order);
    void sendOrderShippedEmail(Order order);
    void sendOrderDeliveredEmail(Order order);
    void sendOrderCancelledEmail(Order order);
    void sendOtpEmail(String toEmail, String otpCode);
    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
