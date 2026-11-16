package com.example.binanceprice.service;

import com.example.binanceprice.client.BinanceClient;
import com.example.binanceprice.client.dto.AvgPriceResponse;
import com.example.binanceprice.client.exception.BinanceException;
import com.example.binanceprice.client.exception.BinanceRateLimitException;
import com.example.binanceprice.config.BinanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricePollingService {

    private final BinanceClient binanceClient;
    private final PriceSnapshotService snapshotService;
    private final PollingPause pause;
    private final BinanceProperties properties;
    private final Clock clock;

    public void pollAll() {
        if (pause.isActive()) {
            log.info("Binance polling is paused until {}", pause.getPausedUntil());
            return;
        }
        for (String symbol : properties.symbols()) {
            try {
                poll(symbol);
            } catch (BinanceRateLimitException e) {
                Instant pausedUntil = pause.pauseFor(e.getRetryAfter());
                log.atLevel(e.getLogLevel()).log("{}; polling paused until {}", e.getMessage(), pausedUntil);
                return;
            } catch (BinanceException e) {
                log.atLevel(e.getLogLevel()).log(e.getMessage());
            } catch (RuntimeException e) {
                log.error("Failed to poll {}", symbol, e);
            }
        }
    }

    private void poll(String symbol) {
        AvgPriceResponse avgPrice = binanceClient.getAvgPrice(symbol);
        Instant fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        snapshotService.save(symbol, avgPrice, fetchedAt);
    }
}
