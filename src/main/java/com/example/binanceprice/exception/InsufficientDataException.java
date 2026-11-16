package com.example.binanceprice.exception;

public class InsufficientDataException extends UnprocessableException {

    public InsufficientDataException(String message) {
        super(message);
    }
}
