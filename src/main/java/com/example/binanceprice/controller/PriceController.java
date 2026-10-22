package com.example.binanceprice.controller;

import com.example.binanceprice.domain.TimeRange;
import com.example.binanceprice.dto.AveragePriceResponse;
import com.example.binanceprice.dto.PricePageResponse;
import com.example.binanceprice.dto.PriceResponse;
import com.example.binanceprice.dto.TrendResponse;
import com.example.binanceprice.service.PriceQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private static final String DEFAULT_SYMBOL = "BNBUSDT";
    private static final int MAX_PAGE_SIZE = 100;

    private final PriceQueryService priceQueryService;

    public PriceController(PriceQueryService priceQueryService) {
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/latest")
    public PriceResponse getLatest(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol) {
        return PriceResponse.from(priceQueryService.getLatest(symbol));
    }

    @GetMapping("/average")
    public AveragePriceResponse getAverage(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                           @RequestParam String from,
                                           @RequestParam String to) {
        return new AveragePriceResponse(priceQueryService.getAveragePrice(symbol, TimeRange.parse(from, to)));
    }

    @GetMapping
    public PricePageResponse getPrices(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                       @RequestParam String from,
                                       @RequestParam String to,
                                       @RequestParam(defaultValue = "0") @PositiveOrZero int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return PricePageResponse.from(priceQueryService.getPrices(symbol, TimeRange.parse(from, to), page, size));
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(@RequestParam(defaultValue = DEFAULT_SYMBOL) String symbol,
                                  @RequestParam String from,
                                  @RequestParam String to) {
        TimeRange range = TimeRange.parse(from, to);
        return TrendResponse.of(symbol, range, priceQueryService.getTrend(symbol, range));
    }
}
