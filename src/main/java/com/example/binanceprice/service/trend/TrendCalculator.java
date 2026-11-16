package com.example.binanceprice.service.trend;

import com.example.binanceprice.config.TrendProperties;
import com.example.binanceprice.dto.response.Trend;
import com.example.binanceprice.exception.InsufficientDataException;
import com.example.binanceprice.repository.projection.TrendAggregates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Classifies a price series by the slope of its least-squares line (x = epoch seconds, y = price),
 * expressed as the relative change over the period: slope * (lastX - firstX) / meanPrice.
 */
@Component
@RequiredArgsConstructor
public class TrendCalculator {

    private static final int MIN_POINTS = 2;

    private final TrendProperties properties;

    public Trend calculate(TrendAggregates sums) {
        if (sums.getCount() < MIN_POINTS) {
            throw new InsufficientDataException("At least two prices are needed for a trend");
        }
        BigDecimal n = BigDecimal.valueOf(sums.getCount());
        BigDecimal slopeDenominator = n.multiply(sums.getSumXX()).subtract(sums.getSumX().multiply(sums.getSumX()));
        if (slopeDenominator.signum() == 0) {
            throw new InsufficientDataException("All prices in the period were fetched at the same time");
        }
        BigDecimal slopeNumerator = n.multiply(sums.getSumXY()).subtract(sums.getSumX().multiply(sums.getSumY()));
        BigDecimal span = sums.getMaxX().subtract(sums.getMinX());
        BigDecimal relativeChange = slopeNumerator.multiply(span).multiply(n)
                .divide(slopeDenominator.multiply(sums.getSumY()), MathContext.DECIMAL128);
        return classify(relativeChange);
    }

    private Trend classify(BigDecimal relativeChange) {
        if (relativeChange.abs().compareTo(properties.flatThreshold()) < 0) {
            return Trend.FLAT;
        }
        return relativeChange.signum() > 0 ? Trend.UPWARD : Trend.DOWNWARD;
    }
}
