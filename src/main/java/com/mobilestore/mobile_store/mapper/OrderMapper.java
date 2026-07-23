package com.mobilestore.mobile_store.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mobilestore.mobile_store.dto.response.OrderItemResponseDto;
import com.mobilestore.mobile_store.dto.response.OrderResponseDto;
import com.mobilestore.mobile_store.entity.Order;
import com.mobilestore.mobile_store.entity.OrderItem;

@Component
public class OrderMapper {

    public OrderResponseDto toResponseDto(Order order) {
        List<OrderItemResponseDto> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            items.add(OrderItemResponseDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProductName())
                    .variantColor(item.getVariantColor())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .build());
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .paymentIntentId(order.getPaymentIntentId())
                .email(order.getCustomer().getEmail())
                .name(order.getCustomer().getName())
                .phone(order.getCustomer().getPhone())
                .address(order.getCustomer().getAddress())
                .city(order.getCustomer().getCity())
                .postalCode(order.getCustomer().getPostalCode())
                .country(order.getCustomer().getCountry())
                .shippingMethod(order.getShippingMethod())
                .shippingCost(order.getShippingCost())
                .subtotal(order.getSubtotal())
                .couponCode(order.getCouponCode())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .status(order.getStatus())
                .instructions(order.getInstructions())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
