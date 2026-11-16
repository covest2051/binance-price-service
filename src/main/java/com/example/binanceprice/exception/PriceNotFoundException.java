package com.example.binanceprice.exception;

import java.time.LocalDate;

public class PriceNotFoundException extends NotFoundException {

    public PriceNotFoundException(String symbol) {
        super("No prices stored for " + symbol);
    }

    public PriceNotFoundException(String symbol, LocalDate from, LocalDate to) {
        super("No prices for %s from %s to %s".formatted(symbol, from, to));
    }
}
