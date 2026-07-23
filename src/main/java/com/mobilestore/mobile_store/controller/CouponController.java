package com.mobilestore.mobile_store.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mobilestore.mobile_store.dto.request.CreateCouponRequestDto;
import com.mobilestore.mobile_store.dto.response.ApiResponseDto;
import com.mobilestore.mobile_store.dto.response.CouponResponseDto;
import com.mobilestore.mobile_store.service.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponseDto<List<CouponResponseDto>>> getAllCoupons() {
        List<CouponResponseDto> response = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponseDto.success("Coupons fetched successfully", response));
    }

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponseDto<CouponResponseDto>> createCoupon(
            @Valid @RequestBody CreateCouponRequestDto request) {
        CouponResponseDto response = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Coupon created successfully", response));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponseDto.success("Coupon deleted successfully", null));
    }

    @GetMapping("/api/coupons/validate/{code}")
    public ResponseEntity<ApiResponseDto<CouponResponseDto>> validateCoupon(
            @PathVariable String code,
            @RequestParam(required = false) BigDecimal subtotal,
            @RequestParam(required = false) String email) {
        CouponResponseDto response = couponService.validateCoupon(code, subtotal, email);
        return ResponseEntity.ok(ApiResponseDto.success("Coupon is valid", response));
    }
}
