package com.carrefour.loyalty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point.
 *
 * <p>This class remains intentionally thin because bootstrapping is a technical concern.
 * The business rules live in the model and service layers.
 */
@SpringBootApplication
public class LoyaltyApplication {

    /**
     * Standard Java main method used by Spring Boot to start the application.
     *
     * @param args runtime arguments supplied on the command line.
     */
    public static void main(String[] args) {
        SpringApplication.run(LoyaltyApplication.class, args);
    }
}
