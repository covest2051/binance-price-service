package com.example.binanceprice.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record PeriodRequest(
        @Parameter(description = "First day of the period, UTC, inclusive", example = "2026-09-01")
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "Last day of the period, UTC, inclusive", example = "2026-09-30")
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {

    @Schema(hidden = true)
    @AssertTrue(message = "from must not be after to")
    public boolean isValidPeriod() {
        return from == null || to == null || !from.isAfter(to);
    }
}
