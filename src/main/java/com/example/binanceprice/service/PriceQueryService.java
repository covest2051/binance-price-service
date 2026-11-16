package com.example.binanceprice.service;

import com.example.binanceprice.config.BinanceProperties;
import com.example.binanceprice.dto.request.PeriodRequest;
import com.example.binanceprice.dto.response.AveragePriceResponse;
import com.example.binanceprice.dto.response.PricePageResponse;
import com.example.binanceprice.dto.response.PriceResponse;
import com.example.binanceprice.dto.response.Trend;
import com.example.binanceprice.dto.response.TrendResponse;
import com.example.binanceprice.exception.PriceNotFoundException;
import com.example.binanceprice.exception.UnsupportedSymbolException;
import com.example.binanceprice.mapper.PriceMapper;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.repository.projection.TrendAggregates;
import com.example.binanceprice.service.trend.TrendCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceQueryService {

    private static final int PRICE_SCALE = 8;

    private final PriceSnapshotRepository repository;
    private final PriceMapper mapper;
    private final TrendCalculator trendCalculator;
    private final BinanceProperties binanceProperties;

    public PriceResponse getLatest(String symbol) {
        requireSupported(symbol);
        return repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(symbol)
                .map(mapper::toResponse)
                .orElseThrow(() -> new PriceNotFoundException(symbol));
    }

    public AveragePriceResponse getAverage(String symbol, PeriodRequest period) {
        requireSupported(symbol);
        BigDecimal average = repository.findAveragePrice(symbol, startOf(period.from()), endExclusive(period.to()))
                .map(value -> value.setScale(PRICE_SCALE, RoundingMode.HALF_EVEN))
                .orElseThrow(() -> new PriceNotFoundException(symbol, period.from(), period.to()));
        return mapper.toAverageResponse(symbol, average);
    }

    public PricePageResponse getPrices(String symbol, PeriodRequest period, int page, int size) {
        requireSupported(symbol);
        return mapper.toPageResponse(repository.findPage(symbol, startOf(period.from()), endExclusive(period.to()),
                PageRequest.of(page, size)));
    }

    public TrendResponse getTrend(String symbol, PeriodRequest period) {
        requireSupported(symbol);
        TrendAggregates aggregates =
                repository.findTrendAggregates(symbol, startOf(period.from()), endExclusive(period.to()));
        if (aggregates.getCount() == 0) {
            throw new PriceNotFoundException(symbol, period.from(), period.to());
        }
        Trend trend = trendCalculator.calculate(aggregates);
        return mapper.toTrendResponse(symbol, period, trend);
    }

    private void requireSupported(String symbol) {
        if (!binanceProperties.symbols().contains(symbol)) {
            throw new UnsupportedSymbolException(symbol, binanceProperties.symbols());
        }
    }

    private static Instant startOf(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant endExclusive(LocalDate lastDay) {
        return startOf(lastDay.plusDays(1));
    }
}
