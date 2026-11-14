CREATE TABLE accounts (
    id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

INSERT INTO accounts (id, balance) VALUES ('acc_123', 1000.00);