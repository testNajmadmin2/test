package com.carrefour.loyalty.dto;

import jakarta.validation.constraints.Min;

/**
 * Request used to earn points from a purchase amount.
 */
public record EarnPointsRequest(
        @Min(value = 100, message = "purchaseAmountInCents must be at least 100") long purchaseAmountInCents) {
}
