package com.carrefour.loyalty.exception;

/**
 * Raised when an operation targets an unknown customer account.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("Customer '%s' was not found".formatted(customerId));
    }
}
