package com.example.binanceprice.exception;

import org.springframework.http.HttpStatus;

public abstract class NotFoundException extends ApplicationException {

    protected NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
