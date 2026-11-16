package com.example.binanceprice.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PollingPause {

    private final Clock clock;

    @Getter
    private volatile Instant pausedUntil = Instant.MIN;

    public boolean isActive() {
        return clock.instant().isBefore(pausedUntil);
    }

    public Instant pauseFor(Duration duration) {
        pausedUntil = clock.instant().plus(duration);
        return pausedUntil;
    }
}
