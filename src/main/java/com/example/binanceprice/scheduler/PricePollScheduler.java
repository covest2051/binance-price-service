package com.example.binanceprice.scheduler;

import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.service.PricePollingService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;

public class PricePollScheduler {

    private static final String LOCK_NAME = "binance-price-poll";

    private final PricePollingService pollingService;
    private final LockingTaskExecutor lockingTaskExecutor;
    private final Clock clock;
    private final Duration lockAtMostFor;
    private final Duration lockAtLeastFor;

    public PricePollScheduler(PricePollingService pollingService, LockingTaskExecutor lockingTaskExecutor,
                              BinanceProperties properties, Clock clock) {
        this.pollingService = pollingService;
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.clock = clock;
        // atMost: a dead node blocks polling for one interval max; atLeast: <= 2 polls per interval across replicas
        this.lockAtMostFor = properties.pollInterval();
        this.lockAtLeastFor = properties.pollInterval().dividedBy(2);
    }

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void poll() {
        lockingTaskExecutor.executeWithLock((Runnable) pollingService::pollAll,
                new LockConfiguration(clock.instant(), LOCK_NAME, lockAtMostFor, lockAtLeastFor));
    }
}
