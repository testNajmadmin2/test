package com.carrefour.loyalty.dto;

/**
 * Response returned after an explicit expiration process.
 */
public record ExpirePointsResponse(String customerId, int expiredPoints, int availablePoints) {
}
