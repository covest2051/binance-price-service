package com.example.binanceprice.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "trend")
public record TrendProperties(@NotNull @PositiveOrZero BigDecimal flatThreshold) {
}
