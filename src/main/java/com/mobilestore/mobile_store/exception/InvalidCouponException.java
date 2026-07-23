package com.mobilestore.mobile_store.exception;

// Covers coupon ineligibility that's a normal, expected outcome and safe to show verbatim to
// the customer: inactive, expired, minimum order not met, usage limit reached, already redeemed
// by this customer. Distinct from CouponNotFoundException (code doesn't exist at all).
public class InvalidCouponException extends RuntimeException {

    public InvalidCouponException(String message) {
        super(message);
    }
}
