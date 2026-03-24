package com.carrefour.loyalty.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("Customer '%s' was not found".formatted(customerId));
    }
}
