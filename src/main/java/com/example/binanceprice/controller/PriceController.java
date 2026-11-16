package com.example.binanceprice.controller;

import com.example.binanceprice.dto.request.PeriodRequest;
import com.example.binanceprice.dto.response.AveragePriceResponse;
import com.example.binanceprice.dto.response.PricePageResponse;
import com.example.binanceprice.dto.response.PriceResponse;
import com.example.binanceprice.dto.response.TrendResponse;
import com.example.binanceprice.service.PriceQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController {

    private static final String DEFAULT_SYMBOL = "BNBUSDT";
    private static final int MAX_PAGE_SIZE = 100;

    private final PriceQueryService priceQueryService;

    @GetMapping("/latest")
    public PriceResponse getLatest(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol) {
        return priceQueryService.getLatest(symbol);
    }

    @GetMapping("/average")
    public AveragePriceResponse getAverage(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                           @Valid @ParameterObject PeriodRequest period) {
        return priceQueryService.getAverage(symbol, period);
    }

    @GetMapping
    public PricePageResponse getPrices(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                       @Valid @ParameterObject PeriodRequest period,
                                       @RequestParam(defaultValue = "0") @PositiveOrZero int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return priceQueryService.getPrices(symbol, period, page, size);
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                  @Valid @ParameterObject PeriodRequest period) {
        return priceQueryService.getTrend(symbol, period);
    }
}
