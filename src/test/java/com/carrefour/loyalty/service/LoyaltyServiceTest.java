package com.carrefour.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carrefour.loyalty.dto.BalanceResponse;
import com.carrefour.loyalty.dto.ExpirePointsResponse;
import com.carrefour.loyalty.exception.InsufficientPointsException;
import com.carrefour.loyalty.model.SpendType;
import com.carrefour.loyalty.repository.InMemoryLoyaltyAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Focused unit tests for the core business rules.
 */
class LoyaltyServiceTest {

    private MutableClock clock;
    private LoyaltyService loyaltyService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyAccountRepository(), clock);
    }

    @Test
    void shouldEarnPoints() {
        BalanceResponse response = loyaltyService.earnPoints("cust-1", 2_500);

        assertThat(response.availablePoints()).isEqualTo(25);
        assertThat(response.buckets()).hasSize(1);
    }

    @Test
    void shouldConsumeOldestBucketsFirst() {
        loyaltyService.earnPoints("cust-1", 20_000);
        clock.plusDays(10);
        loyaltyService.earnPoints("cust-1", 15_000);

        BalanceResponse response = loyaltyService.spendPoints("cust-1", 250, SpendType.PAYMENT);

        assertThat(response.availablePoints()).isEqualTo(100);
        assertThat(response.buckets().get(0).remainingPoints()).isZero();
        assertThat(response.buckets().get(1).remainingPoints()).isEqualTo(100);
    }

    @Test
    void shouldRejectExpiredPoints() {
        loyaltyService.earnPoints("cust-1", 10_000);
        clock.plusDays(LoyaltyService.POINT_VALIDITY_DAYS + 1L);

        assertThatThrownBy(() -> loyaltyService.spendPoints("cust-1", 10, SpendType.PAYMENT))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    void shouldExpirePointsExplicitly() {
        loyaltyService.earnPoints("cust-1", 10_000);
        clock.plusDays(LoyaltyService.POINT_VALIDITY_DAYS + 1L);

        ExpirePointsResponse response = loyaltyService.expirePoints("cust-1");

        assertThat(response.expiredPoints()).isEqualTo(100);
        assertThat(response.availablePoints()).isZero();
    }

    @Test
    void shouldListBucketsExpiringSoon() {
        loyaltyService.earnPoints("cust-1", 10_000);
        clock.plusDays(LoyaltyService.POINT_VALIDITY_DAYS - LoyaltyService.EXPIRING_SOON_DAYS);

        BalanceResponse response = loyaltyService.getExpiringSoon("cust-1");

        assertThat(response.buckets()).hasSize(1);
        assertThat(response.buckets().getFirst().remainingPoints()).isEqualTo(100);
    }

    @Test
    void shouldFailWhenBalanceIsInsufficient() {
        loyaltyService.earnPoints("cust-1", 5_000);

        assertThatThrownBy(() -> loyaltyService.spendPoints("cust-1", 100, SpendType.PAYMENT))
                .isInstanceOf(InsufficientPointsException.class);
    }

    /**
     * Tiny mutable clock to keep date-based tests deterministic.
     */
    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plusDays(long days) {
            instant = instant.plusSeconds(days * 24 * 60 * 60);
        }
    }
}
