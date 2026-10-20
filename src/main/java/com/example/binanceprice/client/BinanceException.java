package com.example.binanceprice.client;

public class BinanceException extends RuntimeException {

    public BinanceException(String message) {
        super(message);
    }

    public BinanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
