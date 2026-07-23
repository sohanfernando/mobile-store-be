package com.mobilestore.mobile_store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.mobilestore.mobile_store.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private Long id;
    private String orderNumber;
    private String paymentIntentId;
    private String email;
    private String name;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String shippingMethod;
    private BigDecimal shippingCost;
    private BigDecimal subtotal;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private OrderStatus status;
    private String instructions;
    private List<OrderItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
