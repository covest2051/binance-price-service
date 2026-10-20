package com.example.binanceprice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BinancePriceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinancePriceServiceApplication.class, args);
    }
}
