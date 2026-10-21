package com.example.binanceprice.config;

import com.example.binanceprice.scheduler.PricePollScheduler;
import com.example.binanceprice.service.PricePollingService;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "binance.polling-enabled", havingValue = "true", matchIfMissing = true)
public class PollingConfig {

    @Bean
    public LockingTaskExecutor lockingTaskExecutor(JdbcTemplate jdbcTemplate) {
        JdbcTemplateLockProvider lockProvider = new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate)
                        .usingDbTime()
                        .build());
        return new DefaultLockingTaskExecutor(lockProvider);
    }

    @Bean
    public PricePollScheduler pricePollScheduler(PricePollingService pollingService,
                                                 LockingTaskExecutor lockingTaskExecutor,
                                                 BinanceProperties properties, Clock clock) {
        return new PricePollScheduler(pollingService, lockingTaskExecutor, properties, clock);
    }
}
