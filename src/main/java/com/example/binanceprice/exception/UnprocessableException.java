package com.example.binanceprice.exception;

import org.springframework.http.HttpStatus;

public abstract class UnprocessableException extends ApplicationException {

    protected UnprocessableException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
