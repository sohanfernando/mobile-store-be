package com.mobilestore.mobile_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mobilestore.mobile_store.entity.CouponRedemption;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    boolean existsByCoupon_IdAndCustomerEmailIgnoreCase(Long couponId, String customerEmail);
}
