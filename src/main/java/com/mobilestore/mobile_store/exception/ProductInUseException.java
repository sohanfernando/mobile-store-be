package com.mobilestore.mobile_store.exception;

public class ProductInUseException extends RuntimeException {

    public ProductInUseException(Long id) {
        super("Product with id " + id + " cannot be deleted because it has existing orders");
    }
}
