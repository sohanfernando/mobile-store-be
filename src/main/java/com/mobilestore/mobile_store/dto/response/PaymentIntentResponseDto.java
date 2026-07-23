package com.mobilestore.mobile_store.dto.response;

import java.math.BigDecimal;

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
public class PaymentIntentResponseDto {

    private String clientSecret;
    private String paymentIntentId;
    private BigDecimal amount;
}
