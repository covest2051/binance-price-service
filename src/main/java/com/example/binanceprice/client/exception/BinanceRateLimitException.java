package com.example.binanceprice.client.exception;

import lombok.Getter;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;

@Getter
public class BinanceRateLimitException extends BinanceException {

    private final HttpStatusCode status;
    private final Duration retryAfter;

    public BinanceRateLimitException(HttpStatusCode status, Duration retryAfter) {
        super("Binance rate limit hit: HTTP %d, retry after %s".formatted(status.value(), retryAfter));
        this.status = status;
        this.retryAfter = retryAfter;
    }

    @Override
    public Level getLogLevel() {
        return status.isSameCodeAs(HttpStatus.I_AM_A_TEAPOT) ? Level.ERROR : Level.WARN;
    }
}
