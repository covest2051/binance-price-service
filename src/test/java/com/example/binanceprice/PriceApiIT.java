package com.example.binanceprice;

import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "binance.polling-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PriceApiIT {

    @Autowired
    private PriceSnapshotRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE price_snapshot");
        insert("100.00000000", "2026-09-01T10:00:00Z");
        insert("101.00000000", "2026-09-15T12:00:00Z");
        insert("102.50000001", "2026-09-30T23:59:59Z");
    }

    @Test
    void averageCoversWholeLastDayAndKeepsPriceScale() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"averagePrice":101.16666667}
                        """, true));
    }

    @Test
    void averageWithoutPricesInPeriodIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void pricesArePagedInFetchOrder() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[
                          {"symbol":"BNBUSDT","price":"100.00000000","fetchedAt":"2026-09-01T10:00:00Z"},
                          {"symbol":"BNBUSDT","price":"101.00000000","fetchedAt":"2026-09-15T12:00:00Z"}],
                         "page":0,"size":2,"totalElements":3,"hasNext":true}
                        """, true));

        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[{"symbol":"BNBUSDT","price":"102.50000001","fetchedAt":"2026-09-30T23:59:59Z"}],
                         "page":1,"size":2,"totalElements":3,"hasNext":false}
                        """, true));
    }

    @Test
    void trendIsCalculatedOverPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"symbol":"BNBUSDT","start_date":"2026-09-01","end_date":"2026-09-30","trend":"UPWARD"}
                        """, true));
    }

    @Test
    void trendWithSinglePriceIsUnprocessable() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend").param("from", "2026-09-01").param("to", "2026-09-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/prices/latest?symbol=DOGEUSDT",
            "/api/v1/prices/average?from=2026-09-02&to=2026-09-01",
            "/api/v1/prices/average?from=yesterday&to=2026-09-01",
            "/api/v1/prices/average?to=2026-09-01",
            "/api/v1/prices?from=2026-09-01&to=2026-09-30&size=101"
    })
    void invalidRequestIsBadRequest(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private void insert(String price, String fetchedAt) {
        Instant fetched = Instant.parse(fetchedAt);
        repository.insertIfAbsent("BNBUSDT", new BigDecimal(price), fetched.minusSeconds(2), fetched);
    }
}
