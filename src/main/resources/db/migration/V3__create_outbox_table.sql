CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    processed BOOLEAN DEFAULT FALSE
);