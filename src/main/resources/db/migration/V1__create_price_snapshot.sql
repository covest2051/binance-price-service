CREATE TABLE price_snapshot
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol     VARCHAR(20)    NOT NULL,
    -- Binance: quoteAssetPrecision <= 8, max maxPrice ~3e10 (BTCIDR)
    price      NUMERIC(20, 8) NOT NULL,
    close_time TIMESTAMPTZ    NOT NULL,
    fetched_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_price_snapshot_symbol_close_time UNIQUE (symbol, close_time)
);

CREATE INDEX idx_price_snapshot_symbol_fetched_at ON price_snapshot (symbol, fetched_at);
