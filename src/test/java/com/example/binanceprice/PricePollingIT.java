package com.example.binanceprice;

import com.example.binanceprice.entity.PriceSnapshot;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.service.PricePollingService;
import com.example.binanceprice.support.MutableClock;
import com.example.binanceprice.support.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "binance.polling-enabled=false",
        "binance.symbols=BNBUSDT,ETHUSDT",
        "binance.read-timeout=PT0.5S"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PricePollingIT {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final String BNB = "BNBUSDT";
    private static final String ETH = "ETHUSDT";
    private static final long CLOSE_TIME_MILLIS = 1792500231169L;

    @RegisterExtension
    static WireMockExtension binance = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void binanceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("binance.base-url", binance::baseUrl);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-20T10:00:00.750Z"));
        }
    }

    @Autowired
    private PricePollingService pollingService;
    @Autowired
    private PriceSnapshotRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE price_snapshot");
        stubAvgPrice(ETH, avgPrice("3900.12345678"));
    }

    @Test
    void storesPolledPriceAndReturnsItAsLatest() throws Exception {
        stubAvgPrice(BNB, avgPrice("731.69700546"));

        pollingService.pollAll();

        PriceSnapshot stored = repository.findFirstBySymbolOrderByFetchedAtDescIdDesc(BNB).orElseThrow();
        assertThat(stored.getCloseTime()).isEqualTo(Instant.ofEpochMilli(CLOSE_TIME_MILLIS));
        String fetchedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"symbol":"BNBUSDT","price":"731.69700546","fetchedAt":"%s"}
                        """.formatted(fetchedAt), true));
    }

    @Test
    void sameCloseTimeIsStoredOnce() {
        stubAvgPrice(BNB, avgPrice("731.69700546"));

        pollingService.pollAll();
        clock.advance(Duration.ofSeconds(30));
        pollingService.pollAll();

        assertThat(countRows(BNB)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("failedResponses")
    void failedPollStoresNothingAndNextPollStores(ResponseDefinitionBuilder failure) {
        stubAvgPrice(BNB, failure);

        pollingService.pollAll();
        assertThat(countRows(BNB)).isZero();

        stubAvgPrice(BNB, avgPrice("731.69700546"));
        pollingService.pollAll();
        assertThat(countRows(BNB)).isEqualTo(1);
    }

    static Stream<Named<ResponseDefinitionBuilder>> failedResponses() {
        return Stream.of(
                Named.of("HTTP 500", serverError()),
                Named.of("read timeout", avgPrice("731.69700546").withFixedDelay(1_000)),
                Named.of("malformed body", okJson("{\"price\":")));
    }

    @Test
    void rateLimitStopsCallsToBinanceUntilRetryAfter() {
        stubAvgPrice(BNB, aResponse().withStatus(429).withHeader("Retry-After", "60"));
        pollingService.pollAll();

        stubAvgPrice(BNB, avgPrice("731.69700546"));
        clock.advance(Duration.ofSeconds(59));
        pollingService.pollAll();

        binance.verify(1, getRequestedFor(urlPathEqualTo(AVG_PRICE_PATH)));
        assertThat(countRows(BNB) + countRows(ETH)).isZero();

        clock.advance(Duration.ofSeconds(1));
        pollingService.pollAll();

        assertThat(countRows(BNB)).isEqualTo(1);
        assertThat(countRows(ETH)).isEqualTo(1);
    }

    @Test
    void invalidSymbolDoesNotBlockOtherSymbols() {
        stubAvgPrice(BNB, aResponse().withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"code\":-1121,\"msg\":\"Invalid symbol.\"}"));

        pollingService.pollAll();

        assertThat(countRows(BNB)).isZero();
        assertThat(countRows(ETH)).isEqualTo(1);
    }

    private static ResponseDefinitionBuilder avgPrice(String price) {
        return okJson("""
                {"mins":5,"price":"%s","closeTime":%d}
                """.formatted(price, CLOSE_TIME_MILLIS));
    }

    private static void stubAvgPrice(String symbol, ResponseDefinitionBuilder response) {
        binance.stubFor(get(urlPathEqualTo(AVG_PRICE_PATH))
                .withQueryParam("symbol", equalTo(symbol))
                .willReturn(response));
    }

    private int countRows(String symbol) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM price_snapshot WHERE symbol = ?", Integer.class, symbol);
    }
}
