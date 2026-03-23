package com.carrefour.loyalty.repository;

import com.carrefour.loyalty.model.CustomerLoyaltyAccount;
import java.util.Optional;

/**
 * Repository abstraction for loyalty accounts.
 */
public interface LoyaltyAccountRepository {

    Optional<CustomerLoyaltyAccount> findById(String customerId);

    CustomerLoyaltyAccount save(CustomerLoyaltyAccount account);
}
