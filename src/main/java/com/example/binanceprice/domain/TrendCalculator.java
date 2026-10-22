package com.example.binanceprice.domain;

import com.example.binanceprice.exception.InsufficientDataException;

import java.math.BigDecimal;
import java.math.MathContext;

public class TrendCalculator {

    private final BigDecimal flatThreshold;

    public TrendCalculator(BigDecimal flatThreshold) {
        this.flatThreshold = flatThreshold;
    }

    public Trend calculate(RegressionSums sums) {
        if (sums.count() < 2) {
            throw new InsufficientDataException("At least two prices are needed for a trend");
        }
        BigDecimal n = BigDecimal.valueOf(sums.count());
        BigDecimal slopeDenominator = n.multiply(sums.sumXX()).subtract(sums.sumX().multiply(sums.sumX()));
        if (slopeDenominator.signum() == 0) {
            throw new InsufficientDataException("All prices in the period were fetched at the same time");
        }
        BigDecimal slopeNumerator = n.multiply(sums.sumXY()).subtract(sums.sumX().multiply(sums.sumY()));
        BigDecimal span = sums.maxX().subtract(sums.minX());

        // change = slope * span / mean, slope = numerator / denominator, mean = sumY / n;
        // one division at the end, everything before it is exact
        BigDecimal relativeChange = slopeNumerator.multiply(span).multiply(n)
                .divide(slopeDenominator.multiply(sums.sumY()), MathContext.DECIMAL128);

        if (relativeChange.abs().compareTo(flatThreshold) < 0) {
            return Trend.FLAT;
        }
        return relativeChange.signum() > 0 ? Trend.UPWARD : Trend.DOWNWARD;
    }
}
