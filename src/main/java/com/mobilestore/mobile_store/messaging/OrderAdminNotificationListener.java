package com.mobilestore.mobile_store.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAdminNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_ADMIN_NOTIFICATION_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Broadcasting new order {} to admin dashboard", event.orderNumber());
        messagingTemplate.convertAndSend("/topic/orders", event);
    }
}
