package com.example.binanceprice.service;

import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.domain.TimeRange;
import com.example.binanceprice.entity.PriceSnapshot;
import com.example.binanceprice.exception.PriceDataNotFoundException;
import com.example.binanceprice.exception.UnsupportedSymbolException;
import com.example.binanceprice.repository.PriceSnapshotRepository;
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

    public PriceQueryService(PriceSnapshotRepository repository, BinanceProperties properties) {
        this.repository = repository;
        this.symbols = properties.symbols();
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

    private void requireSupported(String symbol) {
        if (!symbols.contains(symbol)) {
            throw new UnsupportedSymbolException(symbol, symbols);
        }
    }
}
