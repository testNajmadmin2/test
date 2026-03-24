package com.carrefour.loyalty.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A bucket groups points earned at the same time with the same expiration date.
 */
public class PointBucket {

    private final String id;
    private final int initialPoints;
    private final LocalDate earnedDate;
    private final LocalDate expirationDate;
    private int consumedPoints;
    private int expiredPoints;

    public PointBucket(String id, int initialPoints, LocalDate earnedDate, LocalDate expirationDate) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (initialPoints <= 0) {
            throw new IllegalArgumentException("initialPoints must be positive");
        }
        this.initialPoints = initialPoints;
        this.earnedDate = Objects.requireNonNull(earnedDate, "earnedDate must not be null");
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate must not be null");
    }

    public String getId() {
        return id;
    }

    public int getInitialPoints() {
        return initialPoints;
    }

    public LocalDate getEarnedDate() {
        return earnedDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public int getConsumedPoints() {
        return consumedPoints;
    }

    public int getExpiredPoints() {
        return expiredPoints;
    }

    /**
     * Remaining points in the bucket after consumption and expiration.
     */
    public int remainingPoints() {
        return initialPoints - consumedPoints - expiredPoints;
    }

    /**
     * Buckets are unusable on their expiration date and after.
     */
    public boolean isExpiredOn(LocalDate date) {
        return !date.isBefore(expirationDate);
    }

    /**
     * Consumes as many points as possible from this bucket.
     */
    public int consume(int requestedPoints, LocalDate date) {
        if (requestedPoints <= 0) {
            throw new IllegalArgumentException("requestedPoints must be positive");
        }
        if (isExpiredOn(date)) {
            return 0;
        }
        int consumed = Math.min(requestedPoints, remainingPoints());
        consumedPoints += consumed;
        return consumed;
    }

    /**
     * Expires the remaining points if the bucket reached its expiration date.
     */
    public int expireIfNeeded(LocalDate date) {
        if (!isExpiredOn(date)) {
            return 0;
        }
        int remaining = remainingPoints();
        expiredPoints += remaining;
        return remaining;
    }
}
