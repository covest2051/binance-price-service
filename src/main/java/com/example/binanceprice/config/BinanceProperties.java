package com.example.binanceprice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "binance")
public record BinanceProperties(
        @NotNull URI baseUrl,
        @NotEmpty Set<@NotBlank String> symbols,
        @NotNull @DurationMin(millis = 1) Duration pollInterval,
        @NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration readTimeout,
        @NotNull @DurationMin(millis = 1) Duration defaultRetryAfter
) {
}
