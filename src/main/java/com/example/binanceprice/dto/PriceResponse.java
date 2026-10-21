package com.example.binanceprice.dto;

import com.example.binanceprice.entity.PriceSnapshot;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PriceResponse(
        String symbol,
        String price,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant fetchedAt
) {

    public static PriceResponse from(PriceSnapshot snapshot) {
        return new PriceResponse(snapshot.getSymbol(), snapshot.getPrice().toPlainString(), snapshot.getFetchedAt());
    }
}
