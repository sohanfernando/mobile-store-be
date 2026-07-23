package com.mobilestore.mobile_store.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequestDto {

    @NotBlank(message = "Shipping method is required")
    private String shippingMethod;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequestDto> items;

    private String couponCode;

    // Used only to check per-customer coupon eligibility, so the price quoted here matches
    // what order creation will actually accept once the customer submits the order.
    private String email;
}
