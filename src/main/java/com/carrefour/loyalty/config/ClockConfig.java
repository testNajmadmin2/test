package com.carrefour.loyalty.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Technical configuration that exposes a {@link Clock} bean.
 *
 * <p>Injecting the clock makes every date-based business rule deterministic in tests.
 */
@Configuration
public class ClockConfig {

    /**
     * Creates the system UTC clock used by the application.
     *
     * @return a UTC clock shared by services.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
