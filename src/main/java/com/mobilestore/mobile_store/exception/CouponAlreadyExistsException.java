package com.mobilestore.mobile_store.exception;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String code) {
        super("Coupon already exists: " + code);
    }
}
