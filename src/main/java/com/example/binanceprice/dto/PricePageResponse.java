package com.example.binanceprice.dto;

import com.example.binanceprice.entity.PriceSnapshot;
import org.springframework.data.domain.Page;

import java.util.List;

public record PricePageResponse(List<PriceResponse> items, int page, int size, long totalElements, boolean hasNext) {

    public static PricePageResponse from(Page<PriceSnapshot> page) {
        return new PricePageResponse(
                page.getContent().stream().map(PriceResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext());
    }
}
