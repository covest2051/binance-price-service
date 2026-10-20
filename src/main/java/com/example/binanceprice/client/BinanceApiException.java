package com.example.binanceprice.client;

public class BinanceApiException extends BinanceException {

    private final int status;

    public BinanceApiException(String symbol, int status, String details) {
        super("Binance rejected avgPrice request for %s: HTTP %d, %s".formatted(symbol, status, details));
        this.status = status;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }
}
