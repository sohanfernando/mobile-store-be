package com.mobilestore.mobile_store.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.config.RabbitMQConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderNotificationListener {

    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMATION_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info(
                "Simulated confirmation email sent to {} for order {} ({} item(s), total Rs. {})",
                event.email(), event.orderNumber(), event.itemCount(), event.total()
        );
    }
}
