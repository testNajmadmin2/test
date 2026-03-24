package com.carrefour.loyalty.dto;

public record ExpirePointsResponse(String customerId, int expiredPoints, int availablePoints) {
}
