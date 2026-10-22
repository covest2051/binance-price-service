package com.example.binanceprice.exception;

public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }
}
