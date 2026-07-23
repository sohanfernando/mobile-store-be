package com.mobilestore.mobile_store.service;

import com.mobilestore.mobile_store.dto.request.CreateCouponRequestDto;
import com.mobilestore.mobile_store.dto.response.CouponResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<CouponResponseDto> getAllCoupons();
    CouponResponseDto createCoupon(CreateCouponRequestDto request);
    void deleteCoupon(Long id);
    CouponResponseDto validateCoupon(String code, BigDecimal subtotal, String customerEmail);
}
