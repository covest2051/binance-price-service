package com.example.binanceprice.exception;

import org.springframework.http.HttpStatus;

public abstract class BadRequestException extends ApplicationException {

    protected BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
