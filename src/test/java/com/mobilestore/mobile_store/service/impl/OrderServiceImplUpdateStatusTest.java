package com.mobilestore.mobile_store.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mobilestore.mobile_store.dto.response.OrderStatusUpdateResponseDto;
import com.mobilestore.mobile_store.entity.Customer;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.entity.OrderStatus;
import com.mobilestore.mobile_store.mapper.OrderMapper;
import com.mobilestore.mobile_store.messaging.OrderEventPublisher;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;
import com.mobilestore.mobile_store.repository.CustomerRepository;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.service.EmailService;
import com.mobilestore.mobile_store.service.PaymentService;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplUpdateStatusTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderPricingCalculator orderPricingCalculator;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private EmailService emailService;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, customerRepository, orderPricingCalculator,
                couponRepository, couponRedemptionRepository, paymentService, new OrderMapper(),
                emailService, orderEventPublisher);
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
                .id(1L)
                .orderNumber("ORD-1")
                .status(status)
                .customer(Customer.builder().email("buyer@example.com").build())
                .build();
    }

    @Test
    void shippedStatusReportsEmailSentTrueWhenDeliverySucceeds() {
        Order order = orderWithStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(emailService.sendOrderShippedEmail(order)).thenReturn(true);

        OrderStatusUpdateResponseDto result = service.updateOrderStatus(1L, OrderStatus.SHIPPED);

        assertThat(result.emailSent()).isTrue();
        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void deliveredStatusReportsEmailSentFalseWhenSendFails() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(emailService.sendOrderDeliveredEmail(order)).thenReturn(false);

        OrderStatusUpdateResponseDto result = service.updateOrderStatus(1L, OrderStatus.DELIVERED);

        assertThat(result.emailSent()).isFalse();
    }

    @Test
    void settingTheSameStatusDoesNotTriggerAnEmailAndReportsNull() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderStatusUpdateResponseDto result = service.updateOrderStatus(1L, OrderStatus.DELIVERED);

        assertThat(result.emailSent()).isNull();
        verify(emailService, never()).sendOrderDeliveredEmail(order);
    }

    @Test
    void cancelledStatusUsesTheSynchronousCancelEmailNotTheAsyncVariant() {
        Order order = orderWithStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(emailService.sendOrderCancelledEmail(order)).thenReturn(true);

        OrderStatusUpdateResponseDto result = service.updateOrderStatus(1L, OrderStatus.CANCELLED);

        assertThat(result.emailSent()).isTrue();
        verify(emailService, never()).sendOrderCancelledEmailAsync(order);
    }
}
