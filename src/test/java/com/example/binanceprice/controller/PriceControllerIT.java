package com.example.binanceprice.controller;

import com.example.binanceprice.entity.PriceSnapshot;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import com.example.binanceprice.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "binance.polling-enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PriceControllerIT {

    @Autowired
    private PriceSnapshotRepository repository;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
        insert("100.00000000", "2026-09-01T10:00:00Z");
        insert("101.00000000", "2026-09-15T12:00:00Z");
        insert("102.50000001", "2026-09-30T23:59:59Z");
        insert("200.00000000", "2026-10-01T00:00:00Z");
    }

    @Test
    void latestReturnsMostRecentPrice() throws Exception {
        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"symbol":"BNBUSDT","price":200.00000000,"fetchedAt":"2026-10-01T00:00:00Z"}
                        """, true));
    }

    @Test
    void averageCoversWholeLastDayAndKeepsPriceScale() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"symbol":"BNBUSDT","averagePrice":101.16666667}
                        """, true));
    }

    @Test
    void averageWithoutPricesInPeriodIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No prices for BNBUSDT from 2026-08-01 to 2026-08-31"));
    }

    @Test
    void pricesArePagedInFetchOrder() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[
                          {"symbol":"BNBUSDT","price":100.00000000,"fetchedAt":"2026-09-01T10:00:00Z"},
                          {"symbol":"BNBUSDT","price":101.00000000,"fetchedAt":"2026-09-15T12:00:00Z"}],
                         "page":0,"size":2,"totalElements":3,"hasNext":true}
                        """, true));

        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[{"symbol":"BNBUSDT","price":102.50000001,"fetchedAt":"2026-09-30T23:59:59Z"}],
                         "page":1,"size":2,"totalElements":3,"hasNext":false}
                        """, true));
    }

    @Test
    void emptyPageHasNoItems() throws Exception {
        mockMvc.perform(get("/api/v1/prices").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"items":[],"page":0,"size":20,"totalElements":0,"hasNext":false}
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
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void unsupportedSymbolIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/prices/latest").param("symbol", "DOGEUSDT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Symbol DOGEUSDT is not polled, supported: [BNBUSDT]"));
    }

    private void insert(String price, String fetchedAt) {
        Instant fetched = Instant.parse(fetchedAt);
        repository.save(PriceSnapshot.builder()
                .symbol("BNBUSDT")
                .price(new BigDecimal(price))
                .closeTime(fetched.minusSeconds(2))
                .fetchedAt(fetched)
                .build());
    }
}
