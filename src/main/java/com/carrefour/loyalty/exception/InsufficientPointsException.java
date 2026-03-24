package com.carrefour.loyalty.exception;

public class InsufficientPointsException extends RuntimeException {

    public InsufficientPointsException(String customerId, int points) {
        super("Customer '%s' does not have enough points to spend %d points".formatted(customerId, points));
    }
}
