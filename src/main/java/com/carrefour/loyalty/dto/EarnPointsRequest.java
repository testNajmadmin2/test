package com.carrefour.loyalty.dto;

import jakarta.validation.constraints.Min;

public record EarnPointsRequest(
        @Min(value = 100, message = "purchaseAmountInCents must be at least 100") long purchaseAmountInCents) {
}
