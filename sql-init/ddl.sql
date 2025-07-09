CREATE TABLE IF NOT EXISTS Payment (
   correlation_id TEXT PRIMARY KEY,
   amount REAL NOT NULL,
   processor TEXT NOT NULL,
   timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX CONCURRENTLY idx_payment_window_grp
    ON Payment (processed_at, processor)
    INCLUDE (amount);
