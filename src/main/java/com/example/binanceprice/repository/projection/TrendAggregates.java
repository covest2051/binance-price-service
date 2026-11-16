package com.example.binanceprice.repository.projection;

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
