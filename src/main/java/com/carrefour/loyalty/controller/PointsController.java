package com.carrefour.loyalty.controller;

import com.carrefour.loyalty.dto.BalanceResponse;
import com.carrefour.loyalty.dto.DonatePointsRequest;
import com.carrefour.loyalty.dto.EarnPointsRequest;
import com.carrefour.loyalty.dto.ExpirePointsResponse;
import com.carrefour.loyalty.dto.SpendPointsRequest;
import com.carrefour.loyalty.model.SpendType;
import com.carrefour.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal REST controller exposing the required kata endpoints.
 */
@RestController
@RequestMapping("/customers/{customerId}/points")
public class PointsController {

    private final LoyaltyService loyaltyService;

    public PointsController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/earn")
    public BalanceResponse earnPoints(@PathVariable String customerId,
                                      @Valid @RequestBody EarnPointsRequest request) {
        return loyaltyService.earnPoints(customerId, request.purchaseAmountInCents());
    }

    @PostMapping("/spend")
    public BalanceResponse spendPoints(@PathVariable String customerId,
                                       @Valid @RequestBody SpendPointsRequest request) {
        return loyaltyService.spendPoints(customerId, request.points(), request.spendType());
    }

    @PostMapping("/voucher")
    public BalanceResponse createVoucher(@PathVariable String customerId) {
        return loyaltyService.spendPoints(customerId, LoyaltyService.VOUCHER_POINTS_COST, SpendType.VOUCHER);
    }

    @PostMapping("/donate")
    public BalanceResponse donatePoints(@PathVariable String customerId,
                                        @Valid @RequestBody DonatePointsRequest request) {
        return loyaltyService.spendPoints(customerId, request.points(), SpendType.DONATION);
    }

    @GetMapping("/balance")
    public BalanceResponse getBalance(@PathVariable String customerId) {
        return loyaltyService.getBalance(customerId);
    }

    @GetMapping("/expiring-soon")
    public BalanceResponse getExpiringSoon(@PathVariable String customerId) {
        return loyaltyService.getExpiringSoon(customerId);
    }

    @PostMapping("/expire")
    public ExpirePointsResponse expirePoints(@PathVariable String customerId) {
        return loyaltyService.expirePoints(customerId);
    }
}
