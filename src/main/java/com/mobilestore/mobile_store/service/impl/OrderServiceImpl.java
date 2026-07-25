package com.mobilestore.mobile_store.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mobilestore.mobile_store.dto.request.CreateOrderRequestDto;
import com.mobilestore.mobile_store.dto.response.OrderResponseDto;
import com.mobilestore.mobile_store.entity.Coupon;
import com.mobilestore.mobile_store.entity.CouponRedemption;
import com.mobilestore.mobile_store.entity.Customer;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.entity.OrderItem;
import com.mobilestore.mobile_store.entity.OrderStatus;
import com.mobilestore.mobile_store.entity.ProductColorVariant;
import com.mobilestore.mobile_store.exception.InsufficientStockException;
import com.mobilestore.mobile_store.exception.InvalidCouponException;
import com.mobilestore.mobile_store.exception.OrderNotFoundException;
import com.mobilestore.mobile_store.exception.PaymentException;
import com.mobilestore.mobile_store.mapper.OrderMapper;
import com.mobilestore.mobile_store.messaging.OrderCreatedEvent;
import com.mobilestore.mobile_store.messaging.OrderEventPublisher;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;
import com.mobilestore.mobile_store.repository.CustomerRepository;
import com.mobilestore.mobile_store.repository.OrderRepository;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator.PricingResult;
import com.mobilestore.mobile_store.utility.OrderPricingCalculator.ResolvedOrderItem;
import com.mobilestore.mobile_store.service.OrderService;
import com.mobilestore.mobile_store.service.PaymentService;
import com.mobilestore.mobile_store.service.PaymentService.PaymentIntentResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderPricingCalculator orderPricingCalculator;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final PaymentService paymentService;
    private final OrderMapper orderMapper;
    private final com.mobilestore.mobile_store.service.EmailService emailService;
    private final OrderEventPublisher orderEventPublisher;

    @Override
    public OrderResponseDto createOrder(CreateOrderRequestDto request) {
        if (orderRepository.findByPaymentIntentId(request.getPaymentIntentId()).isPresent()) {
            throw new PaymentException("This payment has already been used to place an order");
        }

        PricingResult pricing = orderPricingCalculator.calculate(request.getShippingMethod(), request.getItems(), request.getCouponCode(), request.getEmail(), true);

        PaymentIntentResult paymentIntent = paymentService.retrievePaymentIntent(request.getPaymentIntentId());
        if (!"succeeded".equals(paymentIntent.status())) {
            throw new PaymentException("Payment was not successful");
        }
        long expectedAmount = PaymentServiceImpl.toSmallestCurrencyUnit(pricing.total());
        if (paymentIntent.amount() == null || expectedAmount != paymentIntent.amount()) {
            throw new PaymentException("Payment amount does not match order total");
        }

        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseGet(() -> Customer.builder().email(request.getEmail()).build());

        customer.setName(request.getFirstName() + " " + request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setAddress(buildFullAddress(request));
        customer.setCity(request.getCity());
        customer.setPostalCode(request.getPostalCode());
        customer.setCountry(request.getCountry());
        customer = customerRepository.save(customer);

        List<OrderItem> orderItems = new ArrayList<>();
        for (ResolvedOrderItem resolvedItem : pricing.items()) {
            ProductColorVariant variant = resolvedItem.variant();

            if (variant.getStockQuantity() < resolvedItem.quantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for " + variant.getProduct().getName() + " (" + variant.getColor() + ")"
                );
            }
            variant.setStockQuantity(variant.getStockQuantity() - resolvedItem.quantity());

            orderItems.add(OrderItem.builder()
                    .product(variant.getProduct())
                    .variant(variant)
                    .productName(variant.getProduct().getName())
                    .variantColor(variant.getColor())
                    .unitPrice(variant.getProduct().getPrice())
                    .quantity(resolvedItem.quantity())
                    .build());
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .paymentIntentId(request.getPaymentIntentId())
                .customer(customer)
                .status(OrderStatus.PROCESSING)
                .shippingMethod(request.getShippingMethod())
                .shippingCost(pricing.shippingCost())
                .subtotal(pricing.subtotal())
                .couponCode(pricing.appliedCoupon() != null ? pricing.appliedCoupon().getCode() : null)
                .discountAmount(pricing.discountAmount())
                .total(pricing.total())
                .instructions(request.getInstructions())
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        if (pricing.appliedCoupon() != null) {
            Coupon coupon = pricing.appliedCoupon();
            coupon.setUsesCount(coupon.getUsesCount() + 1);
            couponRepository.save(coupon);
            couponRedemptionRepository.save(CouponRedemption.builder()
                    .coupon(coupon)
                    .customerEmail(request.getEmail())
                    .orderId(savedOrder.getId())
                    .build());
        }

        orderEventPublisher.publishOrderCreated(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                customer.getEmail(),
                customer.getName(),
                savedOrder.getTotal(),
                savedOrder.getItems().size()
        ));

        // Dispatch Payment Completion email if payment intent exists
        if (request.getPaymentIntentId() != null && !request.getPaymentIntentId().isBlank()) {
            emailService.sendPaymentCompletedEmail(savedOrder);
        }

        return orderMapper.toResponseDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(email).stream()
                .map(orderMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(orderMapper::toResponseDto)
                .toList();
    }

    @Override
    public OrderResponseDto updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);

        // Dispatch transactional email notifications on status changes
        if (previousStatus != status) {
            switch (status) {
                case SHIPPED -> emailService.sendOrderShippedEmail(savedOrder);
                case DELIVERED -> emailService.sendOrderDeliveredEmail(savedOrder);
                case CANCELLED -> emailService.sendOrderCancelledEmail(savedOrder);
                case PROCESSING -> emailService.sendPaymentCompletedEmail(savedOrder);
            }
        }

        return orderMapper.toResponseDto(savedOrder);
    }

    @Override
    public void cancelOrderByPaymentIntentId(String paymentIntentId) {
        orderRepository.findByPaymentIntentId(paymentIntentId).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            Order savedOrder = orderRepository.save(order);
            emailService.sendOrderCancelledEmail(savedOrder);
        });
    }

    private String buildFullAddress(CreateOrderRequestDto request) {
        String apartment = request.getApartment();
        return (apartment != null && !apartment.isBlank())
                ? request.getAddress() + ", " + apartment
                : request.getAddress();
    }

    private String generateOrderNumber() {
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD-" + suffix;
    }
}
