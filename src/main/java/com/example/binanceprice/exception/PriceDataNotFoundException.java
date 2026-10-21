package com.example.binanceprice.exception;

public class PriceDataNotFoundException extends RuntimeException {

    public PriceDataNotFoundException(String message) {
        super(message);
    }
}
