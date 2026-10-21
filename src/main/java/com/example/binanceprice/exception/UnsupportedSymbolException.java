package com.example.binanceprice.exception;

import java.util.Set;

public class UnsupportedSymbolException extends RuntimeException {

    public UnsupportedSymbolException(String symbol, Set<String> supported) {
        super("Symbol %s is not polled, supported: %s".formatted(symbol, supported));
    }
}
