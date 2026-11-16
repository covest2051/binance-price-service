package com.example.binanceprice.dto.response;

import java.util.List;

public record PricePageResponse(List<PriceResponse> items, int page, int size, long totalElements, boolean hasNext) {
}
