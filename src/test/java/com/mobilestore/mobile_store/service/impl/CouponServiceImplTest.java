package com.mobilestore.mobile_store.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mobilestore.mobile_store.entity.Coupon;
import com.mobilestore.mobile_store.entity.DiscountType;
import com.mobilestore.mobile_store.exception.InvalidCouponException;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    private CouponServiceImpl service;

    private Coupon baseCoupon() {
        return Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.TEN)
                .expiryDate(LocalDate.now().plusDays(30))
                .active(true)
                .usesCount(0)
                .build();
    }

    @Test
    void validCouponPassesWithNoConstraints() {
        service = new CouponServiceImpl(couponRepository, couponRedemptionRepository);
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(baseCoupon()));

        var result = service.validateCoupon("SAVE10", null, null);

        assertThat(result.getCode()).isEqualTo("SAVE10");
    }

    @Test
    void rejectsWhenSubtotalBelowMinOrderAmount() {
        service = new CouponServiceImpl(couponRepository, couponRedemptionRepository);
        Coupon coupon = baseCoupon();
        coupon.setMinOrderAmount(BigDecimal.valueOf(500));
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("SAVE10", BigDecimal.valueOf(100), null));
    }

    @Test
    void acceptsWhenSubtotalMeetsMinOrderAmount() {
        service = new CouponServiceImpl(couponRepository, couponRedemptionRepository);
        Coupon coupon = baseCoupon();
        coupon.setMinOrderAmount(BigDecimal.valueOf(500));
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        var result = service.validateCoupon("SAVE10", BigDecimal.valueOf(500), null);

        assertThat(result).isNotNull();
    }

    @Test
    void rejectsWhenCustomerAlreadyRedeemedCoupon() {
        service = new CouponServiceImpl(couponRepository, couponRedemptionRepository);
        Coupon coupon = baseCoupon();
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.existsByCoupon_IdAndCustomerEmailIgnoreCase(1L, "buyer@example.com"))
                .thenReturn(true);

        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("SAVE10", null, "buyer@example.com"));
    }
}
