package com.example.binanceprice.service;

import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.config.TrendProperties;
import com.example.binanceprice.domain.RegressionSums;
import com.example.binanceprice.domain.TimeRange;
import com.example.binanceprice.domain.Trend;
import com.example.binanceprice.domain.TrendCalculator;
import com.example.binanceprice.entity.PriceSnapshot;
import com.example.binanceprice.exception.PriceDataNotFoundException;
import com.example.binanceprice.exception.UnsupportedSymbolException;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.repository.TrendAggregates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Service
public class PriceQueryService {

    // same scale as stored prices, NUMERIC(20, 8)
    private static final int AVERAGE_SCALE = 8;

    private final PriceSnapshotRepository repository;
    private final Set<String> symbols;
    private final TrendCalculator trendCalculator;

    public PriceQueryService(PriceSnapshotRepository repository, BinanceProperties binanceProperties,
                             TrendProperties trendProperties) {
        this.repository = repository;
        this.symbols = binanceProperties.symbols();
        this.trendCalculator = new TrendCalculator(trendProperties.flatThreshold());
    }

    public PriceSnapshot getLatest(String symbol) {
        requireSupported(symbol);
        return repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(symbol)
                .orElseThrow(() -> new PriceDataNotFoundException("No prices stored for " + symbol));
    }

    public BigDecimal getAveragePrice(String symbol, TimeRange range) {
        requireSupported(symbol);
        return repository.findAveragePrice(symbol, range.from(), range.to())
                .map(average -> average.setScale(AVERAGE_SCALE, RoundingMode.HALF_EVEN))
                .orElseThrow(() -> new PriceDataNotFoundException(
                        "No prices for %s between %s and %s".formatted(symbol, range.from(), range.to())));
    }

    public Page<PriceSnapshot> getPrices(String symbol, TimeRange range, int page, int size) {
        requireSupported(symbol);
        return repository.findPage(symbol, range.from(), range.to(), PageRequest.of(page, size));
    }

    public Trend getTrend(String symbol, TimeRange range) {
        requireSupported(symbol);
        TrendAggregates aggregates = repository.findTrendAggregates(symbol, range.from(), range.to());
        if (aggregates.getCount() == 0) {
            throw new PriceDataNotFoundException(
                    "No prices for %s between %s and %s".formatted(symbol, range.from(), range.to()));
        }
        return trendCalculator.calculate(new RegressionSums(aggregates.getCount(),
                aggregates.getSumX(), aggregates.getSumY(), aggregates.getSumXY(), aggregates.getSumXX(),
                aggregates.getMinX(), aggregates.getMaxX()));
    }

    private void requireSupported(String symbol) {
        if (!symbols.contains(symbol)) {
            throw new UnsupportedSymbolException(symbol, symbols);
        }
    }
}
