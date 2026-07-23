package com.mobilestore.mobile_store.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String ORDER_CONFIRMATION_QUEUE = "order.confirmation.queue";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "order.dlx";
    public static final String ORDER_CONFIRMATION_DLQ = "order.confirmation.dlq";

    public static final String ORDER_ADMIN_NOTIFICATION_QUEUE = "order.admin-notification.queue";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(ORDER_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue orderConfirmationQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMATION_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderConfirmationDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMATION_DLQ).build();
    }

    @Bean
    public Binding orderConfirmationBinding() {
        return BindingBuilder.bind(orderConfirmationQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderConfirmationDeadLetterBinding() {
        return BindingBuilder.bind(orderConfirmationDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Queue orderAdminNotificationQueue() {
        return QueueBuilder.durable(ORDER_ADMIN_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding orderAdminNotificationBinding() {
        return BindingBuilder.bind(orderAdminNotificationQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
