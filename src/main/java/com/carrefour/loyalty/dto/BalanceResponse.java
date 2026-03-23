package com.carrefour.loyalty.dto;

import java.util.List;

/**
 * Response returned for balance-oriented endpoints.
 */
public record BalanceResponse(
        String customerId,
        int availablePoints,
        List<PointBucketResponse> buckets) {
}
