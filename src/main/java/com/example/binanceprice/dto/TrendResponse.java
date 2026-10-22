package com.example.binanceprice.dto;

import com.example.binanceprice.domain.TimeRange;
import com.example.binanceprice.domain.Trend;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.ZoneOffset;

// snake_case dates follow the example in the spec, unlike the other responses
public record TrendResponse(
        String symbol,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        Trend trend
) {

    public static TrendResponse of(String symbol, TimeRange range, Trend trend) {
        return new TrendResponse(symbol,
                LocalDate.ofInstant(range.from(), ZoneOffset.UTC),
                LocalDate.ofInstant(range.to(), ZoneOffset.UTC),
                trend);
    }
}
