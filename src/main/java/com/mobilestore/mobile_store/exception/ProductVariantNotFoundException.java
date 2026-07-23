package com.mobilestore.mobile_store.exception;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException(Long id) {
        super("Product color variant with id " + id + " not found");
    }
}
