package com.mobilestore.mobile_store.messaging;

import com.mobilestore.mobile_store.config.RabbitMQConfig;
import com.mobilestore.mobile_store.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMATION_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info(
                "Processing order confirmation notification for {} (order #{}, {} item(s), total Rs. {})",
                event.email(), event.orderNumber(), event.itemCount(), event.total()
        );
        emailService.sendOrderConfirmationEmail(event);
    }
}
