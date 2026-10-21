package com.example.binanceprice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "Binance Price Service",
        version = "v1",
        description = "from/to: ISO-8601 instant (2026-09-01T10:00:00Z) or date (2026-09-01, the whole day in UTC). "
                + "Both bounds are inclusive."))
public class BinancePriceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinancePriceServiceApplication.class, args);
    }
}
