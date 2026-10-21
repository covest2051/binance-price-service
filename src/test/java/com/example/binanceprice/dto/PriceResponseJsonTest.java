package com.example.binanceprice.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class PriceResponseJsonTest {

    @Autowired
    private JacksonTester<PriceResponse> json;

    @Test
    void writesFetchedAtInUtcWithoutFractionAndPriceAsString() throws IOException {
        PriceResponse response =
                new PriceResponse("BNBUSDT", "731.69700546", Instant.parse("2026-10-20T12:43:51.123456789Z"));

        JsonContent<PriceResponse> content = json.write(response);

        assertThat(content).extractingJsonPathStringValue("$.fetchedAt").isEqualTo("2026-10-20T12:43:51Z");
        assertThat(content).extractingJsonPathStringValue("$.price").isEqualTo("731.69700546");
    }
}
