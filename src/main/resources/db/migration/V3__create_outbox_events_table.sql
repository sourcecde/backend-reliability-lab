CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               event_id UUID NOT NULL UNIQUE,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(150) NOT NULL,
                               payload JSONB NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               retry_count INTEGER NOT NULL DEFAULT 0,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               published_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (created_at, id)
    WHERE status = 'PENDING';