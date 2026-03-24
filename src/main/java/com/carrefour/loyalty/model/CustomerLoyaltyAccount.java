package com.carrefour.loyalty.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CustomerLoyaltyAccount {

    private final String customerId;
    private final List<PointBucket> buckets = new ArrayList<>();

    public CustomerLoyaltyAccount(String customerId) {
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<PointBucket> getBuckets() {
        return List.copyOf(buckets);
    }

    public void addBucket(PointBucket bucket) {
        buckets.add(Objects.requireNonNull(bucket, "bucket must not be null"));
    }

        public int availablePoints(LocalDate date) {
        return buckets.stream()
                .filter(bucket -> !bucket.isExpiredOn(date))
                .mapToInt(PointBucket::remainingPoints)
                .sum();
    }

        public int expirePoints(LocalDate date) {
        return buckets.stream().mapToInt(bucket -> bucket.expireIfNeeded(date)).sum();
    }

        public List<PointBucket> expiringSoon(LocalDate date, int withinDays) {
        LocalDate limit = date.plusDays(withinDays);
        return buckets.stream()
                .filter(bucket -> bucket.remainingPoints() > 0)
                .filter(bucket -> !bucket.isExpiredOn(date))
                .filter(bucket -> !bucket.getExpirationDate().isAfter(limit))
                .sorted(Comparator.comparing(PointBucket::getEarnedDate).thenComparing(PointBucket::getExpirationDate))
                .toList();
    }

        public void spendPoints(int points, LocalDate date) {
        if (points <= 0) {
            throw new IllegalArgumentException("points must be positive");
        }
        if (availablePoints(date) < points) {
            throw new IllegalStateException("insufficient points");
        }
        int remaining = points;
        List<PointBucket> orderedBuckets = buckets.stream()
                .filter(bucket -> !bucket.isExpiredOn(date))
                .filter(bucket -> bucket.remainingPoints() > 0)
                .sorted(Comparator.comparing(PointBucket::getEarnedDate).thenComparing(PointBucket::getExpirationDate))
                .toList();

        for (PointBucket bucket : orderedBuckets) {
            if (remaining == 0) {
                break;
            }
            remaining -= bucket.consume(remaining, date);
        }
    }
}
