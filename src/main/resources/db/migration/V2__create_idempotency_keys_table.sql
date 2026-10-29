CREATE TABLE idempotency_keys (
                                  id BIGSERIAL PRIMARY KEY,
                                  idempotency_key UUID NOT NULL UNIQUE,
                                  request_hash VARCHAR(64) NOT NULL,
                                  order_id BIGINT REFERENCES orders(id),
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);