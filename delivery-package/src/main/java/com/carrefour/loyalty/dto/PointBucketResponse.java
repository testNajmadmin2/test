package com.carrefour.loyalty.dto;

import java.time.LocalDate;

public record PointBucketResponse(
        String id,
        int remainingPoints,
        LocalDate earnedDate,
        LocalDate expirationDate) {
}
