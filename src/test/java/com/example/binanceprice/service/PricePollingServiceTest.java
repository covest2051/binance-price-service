package com.example.binanceprice.service;

import com.example.binanceprice.client.AvgPrice;
import com.example.binanceprice.client.BinanceApiException;
import com.example.binanceprice.client.BinanceClient;
import com.example.binanceprice.client.BinanceRateLimitException;
import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePollingServiceTest {

    private static final String FIRST = "BNBUSDT";
    private static final String SECOND = "ETHUSDT";
    private static final AvgPrice AVG_PRICE =
            new AvgPrice(new BigDecimal("731.69700546"), Instant.parse("2026-10-20T09:59:58.123Z"));

    @Mock
    private BinanceClient binanceClient;
    @Mock
    private PriceSnapshotRepository repository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:00.750Z"));
    private PricePollingService service;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties(URI.create("http://localhost"),
                new LinkedHashSet<>(List.of(FIRST, SECOND)), Duration.ofSeconds(30),
                Duration.ofSeconds(3), Duration.ofSeconds(10), Duration.ofMinutes(1));
        service = new PricePollingService(binanceClient, repository, properties, clock);
    }

    @Test
    void storesEverySymbolWithFetchedAtTruncatedToSeconds() {
        when(binanceClient.getAvgPrice(any())).thenReturn(AVG_PRICE);

        service.pollAll();

        Instant fetchedAt = Instant.parse("2026-10-20T10:00:00Z");
        verify(repository).insertIfAbsent(FIRST, AVG_PRICE.price(), AVG_PRICE.closeTime(), fetchedAt);
        verify(repository).insertIfAbsent(SECOND, AVG_PRICE.price(), AVG_PRICE.closeTime(), fetchedAt);
    }

    @Test
    void binanceErrorForOneSymbolDoesNotStopOthers() {
        when(binanceClient.getAvgPrice(FIRST))
                .thenThrow(new BinanceApiException(FIRST, 400, "code -1121, Invalid symbol."));
        when(binanceClient.getAvgPrice(SECOND)).thenReturn(AVG_PRICE);

        service.pollAll();

        verify(repository).insertIfAbsent(eq(SECOND), any(), any(), any());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void databaseErrorForOneSymbolDoesNotStopOthers() {
        when(binanceClient.getAvgPrice(any())).thenReturn(AVG_PRICE);
        when(repository.insertIfAbsent(eq(FIRST), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        service.pollAll();

        verify(repository).insertIfAbsent(eq(SECOND), any(), any(), any());
    }

    @Test
    void rateLimitSkipsBinanceCallsUntilRetryAfter() {
        when(binanceClient.getAvgPrice(FIRST))
                .thenThrow(new BinanceRateLimitException(429, Duration.ofSeconds(60)))
                .thenReturn(AVG_PRICE);
        when(binanceClient.getAvgPrice(SECOND)).thenReturn(AVG_PRICE);

        service.pollAll();
        verify(binanceClient, never()).getAvgPrice(SECOND);

        clock.advance(Duration.ofSeconds(59));
        service.pollAll();
        verify(binanceClient, times(1)).getAvgPrice(FIRST);

        clock.advance(Duration.ofSeconds(1));
        service.pollAll();
        verify(binanceClient, times(2)).getAvgPrice(FIRST);
        verify(binanceClient).getAvgPrice(SECOND);
    }
}
