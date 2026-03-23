package com.carrefour.loyalty.dto;

import jakarta.validation.constraints.Min;

/**
 * Small request body used for charity donation.
 */
public record DonatePointsRequest(
        @Min(value = 1, message = "points must be at least 1") int points) {
}
