package com.example.binanceprice.client;

import com.example.binanceprice.client.dto.AvgPriceResponse;
import com.example.binanceprice.client.dto.BinanceErrorResponse;
import com.example.binanceprice.client.exception.BinanceApiException;
import com.example.binanceprice.client.exception.BinanceException;
import com.example.binanceprice.client.exception.BinanceRateLimitException;
import com.example.binanceprice.config.BinanceProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceClient {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final String USED_WEIGHT_HEADER = "X-MBX-USED-WEIGHT-1M";
    private static final int MAX_LOGGED_BODY_LENGTH = 200;

    private final RestClient binanceRestClient;
    private final BinanceProperties properties;
    private final ObjectMapper objectMapper;

    public AvgPriceResponse getAvgPrice(String symbol) {
        try {
            AvgPriceResponse body = binanceRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(AVG_PRICE_PATH).queryParam("symbol", symbol).build())
                    .exchange((request, response) -> readAvgPrice(symbol, response));
            return requireComplete(symbol, body);
        } catch (RestClientException e) {
            throw new BinanceException("avgPrice request for %s failed: %s".formatted(symbol, e.getMostSpecificCause()), e);
        }
    }

    private AvgPriceResponse readAvgPrice(String symbol, ConvertibleClientHttpResponse response)
            throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (isRateLimited(status)) {
            throw new BinanceRateLimitException(status, retryAfter(response.getHeaders()));
        }
        if (status.isError()) {
            throw new BinanceApiException(symbol, status, errorDetails(response));
        }
        log.debug("Binance used request weight (1m): {}", response.getHeaders().getFirst(USED_WEIGHT_HEADER));
        return response.bodyTo(AvgPriceResponse.class);
    }

    private static boolean isRateLimited(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) || status.isSameCodeAs(HttpStatus.I_AM_A_TEAPOT);
    }

    private Duration retryAfter(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value != null && value.matches("\\d{1,9}")) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        log.warn("Missing or invalid Retry-After header '{}', pausing for default {}",
                value, properties.defaultRetryAfter());
        return properties.defaultRetryAfter();
    }

    private String errorDetails(ConvertibleClientHttpResponse response) {
        String body = response.bodyTo(String.class);
        if (!StringUtils.hasText(body)) {
            return "empty body";
        }
        try {
            BinanceErrorResponse error = objectMapper.readValue(body, BinanceErrorResponse.class);
            return "code %d, %s".formatted(error.code(), error.msg());
        } catch (JacksonException e) {
            log.debug("Binance error body is not a Binance error JSON: {}", e.getOriginalMessage());
            return "body: " + StringUtils.truncate(body, MAX_LOGGED_BODY_LENGTH);
        }
    }

    private static AvgPriceResponse requireComplete(String symbol, AvgPriceResponse body) {
        if (body == null || body.price() == null || body.closeTime() == null) {
            throw new BinanceException("Malformed avgPrice response for %s: %s".formatted(symbol, body));
        }
        return body;
    }
}
