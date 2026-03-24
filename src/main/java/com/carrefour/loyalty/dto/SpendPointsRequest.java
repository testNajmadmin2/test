package com.carrefour.loyalty.dto;

import com.carrefour.loyalty.model.SpendType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SpendPointsRequest(
        @Min(value = 1, message = "points must be at least 1") int points,
        @NotNull(message = "spendType is required") SpendType spendType) {
}
