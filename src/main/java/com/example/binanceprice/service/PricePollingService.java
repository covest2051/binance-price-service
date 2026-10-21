package com.example.binanceprice.service;

import com.example.binanceprice.client.AvgPrice;
import com.example.binanceprice.client.BinanceApiException;
import com.example.binanceprice.client.BinanceClient;
import com.example.binanceprice.client.BinanceException;
import com.example.binanceprice.client.BinanceRateLimitException;
import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
public class PricePollingService {

    private static final Logger log = LoggerFactory.getLogger(PricePollingService.class);

    private final BinanceClient binanceClient;
    private final PriceSnapshotRepository repository;
    private final Set<String> symbols;
    private final Clock clock;

    private volatile Instant pausedUntil = Instant.MIN;

    public PricePollingService(BinanceClient binanceClient, PriceSnapshotRepository repository,
                               BinanceProperties properties, Clock clock) {
        this.binanceClient = binanceClient;
        this.repository = repository;
        this.symbols = properties.symbols();
        this.clock = clock;
    }

    public void pollAll() {
        if (clock.instant().isBefore(pausedUntil)) {
            log.info("Binance polling is paused until {}", pausedUntil);
            return;
        }
        for (String symbol : symbols) {
            try {
                poll(symbol);
            } catch (BinanceRateLimitException e) {
                // limits are per IP, pause all symbols
                pausedUntil = clock.instant().plus(e.getRetryAfter());
                if (e.isIpBanned()) {
                    log.error("{}; polling paused until {}", e.getMessage(), pausedUntil);
                } else {
                    log.warn("{}; polling paused until {}", e.getMessage(), pausedUntil);
                }
                return;
            } catch (BinanceApiException e) {
                if (e.isClientError()) {
                    log.error(e.getMessage());
                } else {
                    log.warn(e.getMessage());
                }
            } catch (BinanceException e) {
                log.warn(e.getMessage());
            } catch (RuntimeException e) {
                log.error("Failed to poll {}", symbol, e);
            }
        }
    }

    private void poll(String symbol) {
        AvgPrice avgPrice = binanceClient.getAvgPrice(symbol);
        // seconds, as in API responses: to=<returned fetchedAt> must include this row
        Instant fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        int inserted = repository.insertIfAbsent(symbol, avgPrice.price(), avgPrice.closeTime(), fetchedAt);
        if (inserted == 0) {
            log.debug("{}: no trades since closeTime {}, nothing stored", symbol, avgPrice.closeTime());
        } else {
            log.info("{}: stored price {} (closeTime {})", symbol, avgPrice.price(), avgPrice.closeTime());
        }
    }
}
