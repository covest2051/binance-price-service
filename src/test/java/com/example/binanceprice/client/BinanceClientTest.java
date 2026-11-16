package com.example.binanceprice.client;

import com.example.binanceprice.client.dto.AvgPriceResponse;
import com.example.binanceprice.client.exception.BinanceApiException;
import com.example.binanceprice.client.exception.BinanceException;
import com.example.binanceprice.client.exception.BinanceRateLimitException;
import com.example.binanceprice.config.BinanceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BinanceClientTest {

    private static final String BASE_URL = "http://binance.test";
    private static final String AVG_PRICE_URL = BASE_URL + "/api/v3/avgPrice?symbol=BNBUSDT";
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofMinutes(1);

    private MockRestServiceServer server;
    private BinanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        BinanceProperties properties = new BinanceProperties(URI.create(BASE_URL), Set.of("BNBUSDT"),
                Duration.ofSeconds(30), Duration.ofSeconds(3), Duration.ofSeconds(10), DEFAULT_RETRY_AFTER);
        client = new BinanceClient(builder.build(), properties, new ObjectMapper());
    }

    @Test
    void parsesAvgPrice() {
        server.expect(requestTo(AVG_PRICE_URL)).andRespond(withSuccess("""
                {"mins":5,"price":"731.69700546","closeTime":1792500231169}
                """, MediaType.APPLICATION_JSON));

        AvgPriceResponse response = client.getAvgPrice("BNBUSDT");

        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("731.69700546"));
        assertThat(response.closeTime()).isEqualTo(1792500231169L);
    }

    @Test
    void responseWithoutPriceIsMalformed() {
        server.expect(requestTo(AVG_PRICE_URL))
                .andRespond(withSuccess("{\"closeTime\":1792500231169}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getAvgPrice("BNBUSDT"))
                .isExactlyInstanceOf(BinanceException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    void binanceErrorBodyIsIncludedInException() {
        server.expect(requestTo(AVG_PRICE_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":-1121,\"msg\":\"Invalid symbol.\"}"));

        assertThatThrownBy(() -> client.getAvgPrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceApiException.class,
                        e -> assertThat(e.getLogLevel()).isEqualTo(Level.ERROR))
                .hasMessageContaining("HTTP 400, code -1121, Invalid symbol.");
    }

    @Test
    void nonJsonErrorBodyIsIncludedAsText() {
        server.expect(requestTo(AVG_PRICE_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.TEXT_HTML)
                .body("<html>Bad gateway</html>"));

        assertThatThrownBy(() -> client.getAvgPrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceApiException.class,
                        e -> assertThat(e.getLogLevel()).isEqualTo(Level.WARN))
                .hasMessageContaining("HTTP 502, body: <html>Bad gateway</html>");
    }

    @Test
    void rateLimitUsesRetryAfterHeader() {
        server.expect(requestTo(AVG_PRICE_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "42"));

        assertThatThrownBy(() -> client.getAvgPrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRateLimitException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofSeconds(42)));
    }

    @Test
    void ipBanWithoutRetryAfterUsesDefaultPauseAndLogsAsError() {
        server.expect(requestTo(AVG_PRICE_URL)).andRespond(withStatus(HttpStatus.I_AM_A_TEAPOT));

        assertThatThrownBy(() -> client.getAvgPrice("BNBUSDT"))
                .isInstanceOfSatisfying(BinanceRateLimitException.class, e -> {
                    assertThat(e.getRetryAfter()).isEqualTo(DEFAULT_RETRY_AFTER);
                    assertThat(e.getLogLevel()).isEqualTo(Level.ERROR);
                });
    }
}
