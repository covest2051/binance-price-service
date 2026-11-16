package com.example.binanceprice.client.dto;

import java.math.BigDecimal;

public record AvgPriceResponse(BigDecimal price, Long closeTime) {
}
