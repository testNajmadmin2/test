package com.carrefour.loyalty.repository;

import com.carrefour.loyalty.model.CustomerLoyaltyAccount;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLoyaltyAccountRepository implements LoyaltyAccountRepository {

    private final ConcurrentHashMap<String, CustomerLoyaltyAccount> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<CustomerLoyaltyAccount> findById(String customerId) {
        return Optional.ofNullable(storage.get(customerId));
    }

    @Override
    public CustomerLoyaltyAccount save(CustomerLoyaltyAccount account) {
        storage.put(account.getCustomerId(), account);
        return account;
    }
}
