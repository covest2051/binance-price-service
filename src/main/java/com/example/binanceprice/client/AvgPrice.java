package com.example.binanceprice.client;

import java.math.BigDecimal;
import java.time.Instant;

public record AvgPrice(BigDecimal price, Instant closeTime) {
}
