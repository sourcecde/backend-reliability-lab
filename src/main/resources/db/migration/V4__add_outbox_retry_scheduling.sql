ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_outbox_events_retry
    ON outbox_events (next_attempt_at, created_at, id)
    WHERE status = 'PENDING';