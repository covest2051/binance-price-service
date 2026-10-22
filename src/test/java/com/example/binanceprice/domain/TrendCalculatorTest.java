package com.example.binanceprice.domain;

import com.example.binanceprice.exception.InsufficientDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrendCalculatorTest {

    private static final long BASE_EPOCH_SECOND = Instant.parse("2026-09-01T00:00:00Z").getEpochSecond();

    private final TrendCalculator calculator = new TrendCalculator(new BigDecimal("0.002"));

    @Test
    void risingPricesAreUpward() {
        Trend trend = calculator.calculate(sums(point(0, "100"), point(30, "100.5"), point(60, "101")));

        assertThat(trend).isEqualTo(Trend.UPWARD);
    }

    @Test
    void fallingPricesAreDownward() {
        Trend trend = calculator.calculate(sums(point(0, "101"), point(30, "100.5"), point(60, "100")));

        assertThat(trend).isEqualTo(Trend.DOWNWARD);
    }

    @Test
    void changeBelowThresholdIsFlat() {
        // 2 * (1001 - 999.001) / (999.001 + 1001) = 0.0019989...
        Trend trend = calculator.calculate(sums(point(0, "999.001"), point(60, "1001")));

        assertThat(trend).isEqualTo(Trend.FLAT);
    }

    @Test
    void changeEqualToThresholdIsNotFlat() {
        // 2 * (1001 - 999) / (999 + 1001) = 0.002
        Trend trend = calculator.calculate(sums(point(0, "999"), point(60, "1001")));

        assertThat(trend).isEqualTo(Trend.UPWARD);
    }

    @Test
    void followsRegressionLineRatherThanFirstAndLastPrice() {
        Trend trend = calculator.calculate(
                sums(point(0, "100"), point(60, "99"), point(120, "130"), point(180, "99.5")));

        assertThat(trend).isEqualTo(Trend.UPWARD);
    }

    @Test
    void singlePriceIsNotEnough() {
        assertThatThrownBy(() -> calculator.calculate(sums(point(0, "100"))))
                .isInstanceOf(InsufficientDataException.class);
    }

    @Test
    void pricesFetchedAtTheSameTimeAreNotEnough() {
        assertThatThrownBy(() -> calculator.calculate(sums(point(0, "100"), point(0, "101"))))
                .isInstanceOf(InsufficientDataException.class);
    }

    private static Point point(long secondsFromBase, String price) {
        return new Point(BigDecimal.valueOf(BASE_EPOCH_SECOND + secondsFromBase), new BigDecimal(price));
    }

    private static RegressionSums sums(Point... points) {
        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumXX = BigDecimal.ZERO;
        BigDecimal minX = points[0].x();
        BigDecimal maxX = points[0].x();
        for (Point point : points) {
            sumX = sumX.add(point.x());
            sumY = sumY.add(point.y());
            sumXY = sumXY.add(point.x().multiply(point.y()));
            sumXX = sumXX.add(point.x().multiply(point.x()));
            minX = minX.min(point.x());
            maxX = maxX.max(point.x());
        }
        return new RegressionSums(points.length, sumX, sumY, sumXY, sumXX, minX, maxX);
    }

    private record Point(BigDecimal x, BigDecimal y) {
    }
}
