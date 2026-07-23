package com.mobilestore.mobile_store.dto.response;

import com.mobilestore.mobile_store.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponseDto {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDate expiryDate;
    private boolean active;
    private Integer maxUses;
    private int usesCount;
    private BigDecimal minOrderAmount;
    private LocalDateTime createdAt;
}
