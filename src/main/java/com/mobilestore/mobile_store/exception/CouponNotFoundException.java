package com.mobilestore.mobile_store.exception;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String code) {
        super("Coupon code not found: " + code);
    }
}
