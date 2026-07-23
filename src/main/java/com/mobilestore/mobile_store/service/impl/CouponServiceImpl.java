package com.mobilestore.mobile_store.service.impl;

import com.mobilestore.mobile_store.dto.request.CreateCouponRequestDto;
import com.mobilestore.mobile_store.dto.response.CouponResponseDto;
import com.mobilestore.mobile_store.entity.Coupon;
import com.mobilestore.mobile_store.exception.CouponAlreadyExistsException;
import com.mobilestore.mobile_store.exception.CouponNotFoundException;
import com.mobilestore.mobile_store.exception.InvalidCouponException;
import com.mobilestore.mobile_store.repository.CouponRedemptionRepository;
import com.mobilestore.mobile_store.repository.CouponRepository;
import com.mobilestore.mobile_store.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    @Override
    public List<CouponResponseDto> getAllCoupons(){
        return couponRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponResponseDto createCoupon(CreateCouponRequestDto request){
        if(couponRepository.existsByCodeIgnoreCase(request.getCode())){
            throw new CouponAlreadyExistsException(request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase().trim())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .expiryDate(request.getExpiryDate())
                .active(true)
                .maxUses(request.getMaxUses())
                .minOrderAmount(request.getMinOrderAmount())
                .build();

        Coupon saved = couponRepository.save(coupon);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    @Override
    public CouponResponseDto validateCoupon(String code, BigDecimal subtotal, String customerEmail) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CouponNotFoundException(code));
        if (!coupon.isActive()) {
            throw new InvalidCouponException("Coupon is currently inactive");
        }
        if (coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new InvalidCouponException("Coupon has expired");
        }
        if (coupon.getMaxUses() != null && coupon.getUsesCount() >= coupon.getMaxUses()) {
            throw new InvalidCouponException("Coupon has reached its usage limit");
        }
        if (coupon.getMinOrderAmount() != null && subtotal != null
                && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new InvalidCouponException(
                    "This coupon requires a minimum order of Rs. " + coupon.getMinOrderAmount());
        }
        if (customerEmail != null && !customerEmail.isBlank()
                && couponRedemptionRepository.existsByCoupon_IdAndCustomerEmailIgnoreCase(coupon.getId(), customerEmail)) {
            throw new InvalidCouponException("You have already used this coupon");
        }
        return mapToResponseDto(coupon);
    }

    private CouponResponseDto mapToResponseDto(Coupon coupon) {
        return CouponResponseDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.isActive())
                .maxUses(coupon.getMaxUses())
                .usesCount(coupon.getUsesCount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
