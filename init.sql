CREATE TABLE IF NOT EXISTS Payment
(
    id             SERIAL PRIMARY KEY,
    correlation_id VARCHAR(36),
    amount         DECIMAL(15, 2) NOT NULL,
    processor      VARCHAR(20)    NOT NULL,
    timestamp      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX CONCURRENTLY idx_payment_window_grp
    ON Payment (timestamp, processor)
    INCLUDE (amount);
