package com.example.binanceprice.repository;

import java.math.BigDecimal;

public interface TrendAggregates {

    long getCount();

    BigDecimal getSumX();

    BigDecimal getSumY();

    BigDecimal getSumXY();

    BigDecimal getSumXX();

    BigDecimal getMinX();

    BigDecimal getMaxX();
}
