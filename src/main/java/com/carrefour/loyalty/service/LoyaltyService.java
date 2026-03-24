package com.carrefour.loyalty.service;

import com.carrefour.loyalty.dto.BalanceResponse;
import com.carrefour.loyalty.dto.ExpirePointsResponse;
import com.carrefour.loyalty.dto.PointBucketResponse;
import com.carrefour.loyalty.exception.CustomerNotFoundException;
import com.carrefour.loyalty.exception.InsufficientPointsException;
import com.carrefour.loyalty.model.CustomerLoyaltyAccount;
import com.carrefour.loyalty.model.PointBucket;
import com.carrefour.loyalty.model.SpendType;
import com.carrefour.loyalty.repository.LoyaltyAccountRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LoyaltyService {

    public static final int CENTS_PER_POINT = 100;
    public static final int POINT_VALIDITY_DAYS = 365;
    public static final int EXPIRING_SOON_DAYS = 30;
    public static final int VOUCHER_POINTS_COST = 100;

    private final LoyaltyAccountRepository repository;
    private final Clock clock;

    public LoyaltyService(LoyaltyAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

        public BalanceResponse earnPoints(String customerId, long purchaseAmountInCents) {
        int points = Math.toIntExact(purchaseAmountInCents / CENTS_PER_POINT);
        CustomerLoyaltyAccount account = repository.findById(customerId)
                .orElseGet(() -> new CustomerLoyaltyAccount(customerId));
        LocalDate today = today();
        account.addBucket(new PointBucket(
                UUID.randomUUID().toString(),
                points,
                today,
                today.plusDays(POINT_VALIDITY_DAYS)));
        repository.save(account);
        return toBalanceResponse(account, today);
    }

        public BalanceResponse spendPoints(String customerId, int points, SpendType spendType) {
        CustomerLoyaltyAccount account = getAccount(customerId);
        LocalDate today = today();
        account.expirePoints(today);

        int actualPoints = switch (spendType) {
            case PAYMENT, DONATION -> points;
            case VOUCHER -> VOUCHER_POINTS_COST;
        };

        try {
            account.spendPoints(actualPoints, today);
        } catch (IllegalStateException exception) {
            throw new InsufficientPointsException(customerId, actualPoints);
        }
        repository.save(account);
        return toBalanceResponse(account, today);
    }

        public BalanceResponse getBalance(String customerId) {
        CustomerLoyaltyAccount account = getAccount(customerId);
        LocalDate today = today();
        account.expirePoints(today);
        repository.save(account);
        return toBalanceResponse(account, today);
    }

        public BalanceResponse getExpiringSoon(String customerId) {
        CustomerLoyaltyAccount account = getAccount(customerId);
        LocalDate today = today();
        account.expirePoints(today);
        repository.save(account);
        List<PointBucketResponse> expiringSoon = account.expiringSoon(today, EXPIRING_SOON_DAYS).stream()
                .map(this::toBucketResponse)
                .toList();
        return new BalanceResponse(customerId, account.availablePoints(today), expiringSoon);
    }

        public ExpirePointsResponse expirePoints(String customerId) {
        CustomerLoyaltyAccount account = getAccount(customerId);
        LocalDate today = today();
        int expiredPoints = account.expirePoints(today);
        repository.save(account);
        return new ExpirePointsResponse(customerId, expiredPoints, account.availablePoints(today));
    }

    private CustomerLoyaltyAccount getAccount(String customerId) {
        return repository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private BalanceResponse toBalanceResponse(CustomerLoyaltyAccount account, LocalDate date) {
        return new BalanceResponse(
                account.getCustomerId(),
                account.availablePoints(date),
                account.getBuckets().stream().map(this::toBucketResponse).toList());
    }

    private PointBucketResponse toBucketResponse(PointBucket bucket) {
        return new PointBucketResponse(
                bucket.getId(),
                bucket.remainingPoints(),
                bucket.getEarnedDate(),
                bucket.getExpirationDate());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
