package com.example.binanceprice.domain;

import com.example.binanceprice.exception.InvalidTimeRangeException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

public record TimeRange(Instant from, Instant to) {

    public TimeRange {
        if (from.isAfter(to)) {
            throw new InvalidTimeRangeException("from (%s) is after to (%s)".formatted(from, to));
        }
    }

    public static TimeRange parse(String from, String to) {
        return new TimeRange(
                parseBound("from", from, date -> date.atStartOfDay(ZoneOffset.UTC).toInstant()),
                parseBound("to", to, TimeRange::endOfDay));
    }

    private static Instant parseBound(String name, String value, Function<LocalDate, Instant> dateToInstant) {
        try {
            return value.contains("T") ? Instant.parse(value) : dateToInstant.apply(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidTimeRangeException(
                    "%s must be an ISO-8601 instant (2026-09-01T10:00:00Z) or date (2026-09-01), got '%s'"
                            .formatted(name, value));
        }
    }

    // micros, not nanos: timestamptz would round 23:59:59.999999999 up to the next midnight
    private static Instant endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minus(1, ChronoUnit.MICROS);
    }
}
