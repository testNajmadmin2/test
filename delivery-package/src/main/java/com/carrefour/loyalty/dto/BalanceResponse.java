package com.carrefour.loyalty.dto;

import java.util.List;

public record BalanceResponse(
        String customerId,
        int availablePoints,
        List<PointBucketResponse> buckets) {
}
