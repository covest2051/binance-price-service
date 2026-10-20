package com.example.binanceprice.client;

import com.example.binanceprice.config.BinanceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Component
public class BinanceClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final String USED_WEIGHT_HEADER = "X-MBX-USED-WEIGHT-1M";

    private final RestClient restClient;
    private final Duration defaultRetryAfter;

    public BinanceClient(RestClient binanceRestClient, BinanceProperties properties) {
        this.restClient = binanceRestClient;
        this.defaultRetryAfter = properties.defaultRetryAfter();
    }

    public AvgPrice getAvgPrice(String symbol) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(AVG_PRICE_PATH).queryParam("symbol", symbol).build())
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.isError()) {
                            throw toException(symbol, status, response);
                        }
                        log.debug("Binance used request weight (1m): {}", response.getHeaders().getFirst(USED_WEIGHT_HEADER));
                        return toAvgPrice(symbol, response.bodyTo(AvgPriceResponse.class));
                    });
        } catch (RestClientException e) {
            throw new BinanceException("avgPrice request for %s failed: %s".formatted(symbol, e.getMostSpecificCause()), e);
        }
    }

    private BinanceException toException(String symbol, HttpStatusCode status, ConvertibleClientHttpResponse response) {
        // 418: IP banned for ignoring 429
        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) || status.isSameCodeAs(HttpStatus.I_AM_A_TEAPOT)) {
            return new BinanceRateLimitException(status.value(), retryAfter(response.getHeaders()));
        }
        return new BinanceApiException(symbol, status.value(), errorDetails(response));
    }

    private Duration retryAfter(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value != null && value.matches("\\d{1,9}")) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        log.warn("Missing or invalid Retry-After header '{}', pausing for default {}", value, defaultRetryAfter);
        return defaultRetryAfter;
    }

    private static String errorDetails(ConvertibleClientHttpResponse response) {
        try {
            BinanceErrorResponse error = response.bodyTo(BinanceErrorResponse.class);
            if (error != null) {
                return "code %d, %s".formatted(error.code(), error.msg());
            }
        } catch (RestClientException ignored) {
            // WAF/CDN errors come as HTML
        }
        return "no Binance error body";
    }

    private static AvgPrice toAvgPrice(String symbol, AvgPriceResponse body) {
        if (body == null || body.price() == null || body.closeTime() == null) {
            throw new BinanceException("Malformed avgPrice response for %s: %s".formatted(symbol, body));
        }
        try {
            return new AvgPrice(new BigDecimal(body.price()), Instant.ofEpochMilli(body.closeTime()));
        } catch (NumberFormatException e) {
            throw new BinanceException("Malformed avgPrice price for %s: '%s'".formatted(symbol, body.price()), e);
        }
    }
}
