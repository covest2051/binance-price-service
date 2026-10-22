package com.example.binanceprice.repository;

import com.example.binanceprice.entity.PriceSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PriceSnapshotRepository extends Repository<PriceSnapshot, Long> {

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO price_snapshot (symbol, price, close_time, fetched_at)
            VALUES (:symbol, :price, :closeTime, :fetchedAt)
            ON CONFLICT (symbol, close_time) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(String symbol, BigDecimal price, Instant closeTime, Instant fetchedAt);

    Optional<PriceSnapshot> findFirstBySymbolOrderByFetchedAtDescIdDesc(String symbol);

    @Query("""
            SELECT p FROM PriceSnapshot p
            WHERE p.symbol = :symbol AND p.fetchedAt BETWEEN :from AND :to
            ORDER BY p.fetchedAt, p.id
            """)
    Page<PriceSnapshot> findPage(String symbol, Instant from, Instant to, Pageable pageable);

    // native: JPQL avg() returns Double
    @Query(value = """
            SELECT AVG(price) FROM price_snapshot
            WHERE symbol = :symbol AND fetched_at BETWEEN :from AND :to
            """, nativeQuery = true)
    Optional<BigDecimal> findAveragePrice(String symbol, Instant from, Instant to);

    @Query(value = """
            SELECT COUNT(*)       AS "count",
                   SUM(x)         AS "sumX",
                   SUM(price)     AS "sumY",
                   SUM(x * price) AS "sumXY",
                   SUM(x * x)     AS "sumXX",
                   MIN(x)         AS "minX",
                   MAX(x)         AS "maxX"
            FROM (SELECT EXTRACT(EPOCH FROM fetched_at) AS x, price
                  FROM price_snapshot
                  WHERE symbol = :symbol AND fetched_at BETWEEN :from AND :to) points
            """, nativeQuery = true)
    TrendAggregates findTrendAggregates(String symbol, Instant from, Instant to);
}
