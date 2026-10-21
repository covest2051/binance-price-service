package com.example.binanceprice.domain;

import com.example.binanceprice.exception.InvalidTimeRangeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    @Test
    void parsesInstants() {
        TimeRange range = TimeRange.parse("2026-09-01T10:00:00Z", "2026-09-01T12:30:00Z");

        assertThat(range.from()).isEqualTo("2026-09-01T10:00:00Z");
        assertThat(range.to()).isEqualTo("2026-09-01T12:30:00Z");
    }

    @Test
    void datesCoverWholeDaysInUtc() {
        TimeRange range = TimeRange.parse("2026-09-01", "2026-09-30");

        assertThat(range.from()).isEqualTo("2026-09-01T00:00:00Z");
        assertThat(range.to()).isEqualTo("2026-09-30T23:59:59.999999Z");
    }

    @Test
    void acceptsDateAndInstantTogether() {
        TimeRange range = TimeRange.parse("2026-09-01", "2026-09-01T12:00:00Z");

        assertThat(range.from()).isEqualTo("2026-09-01T00:00:00Z");
        assertThat(range.to()).isEqualTo("2026-09-01T12:00:00Z");
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> TimeRange.parse("2026-09-02", "2026-09-01"))
                .isInstanceOf(InvalidTimeRangeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "yesterday", "2026-13-01", "01.09.2026", "2026-09-01T10:00:00"})
    void rejectsMalformedBound(String from) {
        assertThatThrownBy(() -> TimeRange.parse(from, "2026-09-30"))
                .isInstanceOf(InvalidTimeRangeException.class)
                .hasMessageContaining("from");
    }
}
