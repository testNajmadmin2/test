package com.carrefour.loyalty.dto;

import java.time.LocalDate;

/**
 * API view of a point bucket.
 */
public record PointBucketResponse(
        String id,
        int remainingPoints,
        LocalDate earnedDate,
        LocalDate expirationDate) {
}
