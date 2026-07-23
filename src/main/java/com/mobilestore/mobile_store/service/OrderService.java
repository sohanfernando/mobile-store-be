package com.mobilestore.mobile_store.service;

import java.util.List;

import com.mobilestore.mobile_store.dto.request.CreateOrderRequestDto;
import com.mobilestore.mobile_store.dto.response.OrderResponseDto;
import com.mobilestore.mobile_store.entity.OrderStatus;

public interface OrderService {

    OrderResponseDto createOrder(CreateOrderRequestDto request);

    List<OrderResponseDto> getOrdersByEmail(String email);

    List<OrderResponseDto> getAllOrders();

    OrderResponseDto updateOrderStatus(Long id, OrderStatus status);

    void cancelOrderByPaymentIntentId(String paymentIntentId);
}
