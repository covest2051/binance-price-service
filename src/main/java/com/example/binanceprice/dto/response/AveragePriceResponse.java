package com.example.binanceprice.dto.response;

import java.math.BigDecimal;

public record AveragePriceResponse(String symbol, BigDecimal averagePrice) {
}
