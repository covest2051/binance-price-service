package com.example.binanceprice.client.exception;

import org.slf4j.event.Level;

public class BinanceException extends RuntimeException {

    public BinanceException(String message) {
        super(message);
    }

    public BinanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public Level getLogLevel() {
        return Level.WARN;
    }
}
