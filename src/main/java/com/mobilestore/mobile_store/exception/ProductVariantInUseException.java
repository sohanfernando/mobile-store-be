package com.mobilestore.mobile_store.exception;

public class ProductVariantInUseException extends RuntimeException {

    public ProductVariantInUseException() {
        super("One or more color variants cannot be removed because they have existing orders");
    }
}
