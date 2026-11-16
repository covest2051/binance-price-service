package com.example.binanceprice.scheduler;

import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.service.PricePollingService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "binance.polling-enabled", havingValue = "true", matchIfMissing = true)
public class PricePollScheduler {

    private static final String LOCK_NAME = "binance-price-poll";

    private final PricePollingService pollingService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final BinanceProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void poll() {
        lockingTaskExecutor.executeWithLock((Runnable) pollingService::pollAll, lockConfiguration());
    }

    private LockConfiguration lockConfiguration() {
        Duration interval = properties.pollInterval();
        return new LockConfiguration(clock.instant(), LOCK_NAME, interval, interval.dividedBy(2));
    }
}
