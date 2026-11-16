package com.example.binanceprice.service;

import com.example.binanceprice.client.dto.AvgPriceResponse;
import com.example.binanceprice.entity.PriceSnapshot;
import com.example.binanceprice.mapper.PriceMapper;
import com.example.binanceprice.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceSnapshotService {

    private final PriceSnapshotRepository repository;
    private final PriceMapper mapper;

    @Transactional
    public void save(String symbol, AvgPriceResponse avgPrice, Instant fetchedAt) {
        PriceSnapshot snapshot = mapper.toEntity(symbol, avgPrice, fetchedAt);
        if (repository.insertIfAbsent(snapshot) == 0) {
            log.debug("{}: no trades since closeTime {}, nothing stored", symbol, snapshot.getCloseTime());
            return;
        }
        log.info("{}: stored price {} (closeTime {})", symbol, snapshot.getPrice(), snapshot.getCloseTime());
    }
}
