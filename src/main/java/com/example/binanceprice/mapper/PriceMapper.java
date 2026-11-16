package com.example.binanceprice.mapper;

import com.example.binanceprice.client.dto.AvgPriceResponse;
import com.example.binanceprice.dto.request.PeriodRequest;
import com.example.binanceprice.dto.response.AveragePriceResponse;
import com.example.binanceprice.dto.response.PricePageResponse;
import com.example.binanceprice.dto.response.PriceResponse;
import com.example.binanceprice.dto.response.Trend;
import com.example.binanceprice.dto.response.TrendResponse;
import com.example.binanceprice.entity.PriceSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper
public interface PriceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", source = "response.price")
    @Mapping(target = "closeTime", source = "response.closeTime")
    PriceSnapshot toEntity(String symbol, AvgPriceResponse response, Instant fetchedAt);

    PriceResponse toResponse(PriceSnapshot snapshot);

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "page", source = "number")
    @Mapping(target = "hasNext", expression = "java(page.hasNext())")
    PricePageResponse toPageResponse(Page<PriceSnapshot> page);

    AveragePriceResponse toAverageResponse(String symbol, BigDecimal averagePrice);

    @Mapping(target = "startDate", source = "period.from")
    @Mapping(target = "endDate", source = "period.to")
    TrendResponse toTrendResponse(String symbol, PeriodRequest period, Trend trend);

    default Instant epochMillisToInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }
}
