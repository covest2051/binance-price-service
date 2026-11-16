package com.example.binanceprice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(String symbol, BigDecimal price, Instant fetchedAt) {
}
