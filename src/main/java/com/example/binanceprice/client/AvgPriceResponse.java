package com.example.binanceprice.client;

// closeTime stays Long: Jackson reads a numeric Instant as epoch seconds, Binance sends millis
record AvgPriceResponse(String price, Long closeTime) {
}
