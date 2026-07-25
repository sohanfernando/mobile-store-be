package com.mobilestore.mobile_store.service.impl;

import com.mobilestore.mobile_store.messaging.OrderCreatedEvent;
import com.mobilestore.mobile_store.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:TechPulse Store <onboarding@resend.dev>}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void sendOrderConfirmationEmail(OrderCreatedEvent event) {
        String subject = "Order Confirmation #" + event.orderNumber() + " - TechPulse Store";
        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f8f7; margin: 0; padding: 20px; color: #171717; }
                    .card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 24px; padding: 32px; border: 1px solid #eaeaea; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
                    .header { text-align: center; padding-bottom: 24px; border-bottom: 1px solid #eaeaea; }
                    .title { font-size: 24px; font-weight: 900; text-transform: uppercase; color: #FF6600; margin: 0; }
                    .subtitle { font-size: 12px; color: #6B6B68; margin-top: 4px; text-transform: uppercase; letter-spacing: 1px; }
                    .content { padding: 24px 0; line-height: 1.6; }
                    .order-box { background: #f8f8f7; border-radius: 16px; padding: 20px; margin: 20px 0; border: 1px solid #eaeaea; }
                    .order-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
                    .total-row { font-size: 18px; font-weight: 900; color: #FF6600; border-top: 1px solid #eaeaea; padding-top: 12px; margin-top: 12px; }
                    .footer { text-align: center; font-size: 12px; color: #6B6B68; padding-top: 20px; border-top: 1px solid #eaeaea; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1 class="title">TechPulse</h1>
                        <p class="subtitle">Trusted Mobile Store</p>
                    </div>
                    <div class="content">
                        <h2>Thank you for your order, %s!</h2>
                        <p>We've received your order and are processing it right now. Below are your order summary details:</p>
                        
                        <div class="order-box">
                            <div class="order-row"><span>Order Number:</span> <strong>#%s</strong></div>
                            <div class="order-row"><span>Total Items:</span> <strong>%d item(s)</strong></div>
                            <div class="order-row total-row"><span>Total Amount:</span> <strong>Rs. %,.2f</strong></div>
                        </div>

                        <p>You can track your order status anytime by logging into your TechPulse account dashboard.</p>
                    </div>
                    <div class="footer">
                        <p>Need help? Contact us at support@techpulse.lk</p>
                        <p>&copy; 2026 TechPulse Mobile Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                event.name() != null && !event.name().isBlank() ? event.name() : "Customer",
                event.orderNumber(),
                event.itemCount(),
                event.total()
        );

        sendHtmlEmail(event.email(), subject, htmlBody);
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        String subject = "Your TechPulse Verification Code: " + otpCode;
        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f8f7; margin: 0; padding: 20px; color: #171717; }
                    .card { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 24px; padding: 32px; border: 1px solid #eaeaea; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
                    .header { text-align: center; padding-bottom: 20px; border-bottom: 1px solid #eaeaea; }
                    .title { font-size: 24px; font-weight: 900; text-transform: uppercase; color: #FF6600; margin: 0; }
                    .subtitle { font-size: 11px; color: #6B6B68; margin-top: 4px; text-transform: uppercase; letter-spacing: 1px; }
                    .content { padding: 24px 0; text-align: center; }
                    .otp-box { background: #f8f8f7; border-radius: 16px; padding: 20px; margin: 20px 0; border: 1px solid #eaeaea; letter-spacing: 8px; font-size: 32px; font-weight: 900; color: #FF6600; font-family: monospace; }
                    .footer { text-align: center; font-size: 11px; color: #6B6B68; padding-top: 20px; border-top: 1px solid #eaeaea; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1 class="title">TechPulse</h1>
                        <p class="subtitle">Verification Passcode</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 14px; color: #6B6B68;">Your 6-digit verification code for TechPulse sign-in is:</p>
                        <div class="otp-box">%s</div>
                        <p style="font-size: 12px; color: #6B6B68;">This code is valid for 10 minutes. If you did not request this code, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 TechPulse Mobile Store. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otpCode);

        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend API Key (RESEND_API_KEY) is not set. Email notification skipped for {}", toEmail);
            return;
        }

        try {
            String jsonPayload = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":%s}",
                    escapeJson(fromEmail),
                    escapeJson(toEmail),
                    escapeJson(subject),
                    toJsonString(htmlBody)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("Email sent successfully via Resend to {}. Response: {}", toEmail, response.body());
            } else {
                log.error("Failed to send email via Resend to {}. Status: {}, Response: {}", toEmail, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error sending email via Resend API to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String toJsonString(String input) {
        if (input == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
