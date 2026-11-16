package com.example.binanceprice.client.exception;

import org.slf4j.event.Level;
import org.springframework.http.HttpStatusCode;

public class BinanceApiException extends BinanceException {

    private final HttpStatusCode status;

    public BinanceApiException(String symbol, HttpStatusCode status, String details) {
        super("Binance rejected avgPrice request for %s: HTTP %d, %s".formatted(symbol, status.value(), details));
        this.status = status;
    }

    @Override
    public Level getLogLevel() {
        return status.is4xxClientError() ? Level.ERROR : Level.WARN;
    }
}
