package com.example.binanceprice.client;

import org.springframework.http.HttpStatus;

import java.time.Duration;

public class BinanceRateLimitException extends BinanceException {

    private final int status;
    private final Duration retryAfter;

    public BinanceRateLimitException(int status, Duration retryAfter) {
        super("Binance rate limit hit: HTTP %d, retry after %s".formatted(status, retryAfter));
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public boolean isIpBanned() {
        return status == HttpStatus.I_AM_A_TEAPOT.value();
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
