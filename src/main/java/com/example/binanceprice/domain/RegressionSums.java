package com.example.binanceprice.domain;

import java.math.BigDecimal;

// x = epoch seconds of fetchedAt, y = price
public record RegressionSums(
        long count,
        BigDecimal sumX,
        BigDecimal sumY,
        BigDecimal sumXY,
        BigDecimal sumXX,
        BigDecimal minX,
        BigDecimal maxX
) {
}
