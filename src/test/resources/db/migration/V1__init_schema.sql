-- V1__init_schema.sql
-- Multi-Currency Digital Wallet Schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- Accounts table
CREATE TABLE accounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number  VARCHAR(20)     NOT NULL UNIQUE,
    user_id         UUID            NOT NULL REFERENCES users(id),
    balance         NUMERIC(20, 3)  NOT NULL DEFAULT 0.000,
    currency_code   VARCHAR(3)      NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_currency_length      CHECK (char_length(currency_code) = 3)
);

CREATE INDEX idx_accounts_user_id       ON accounts(user_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_currency      ON accounts(currency_code);

-- Transactions table (immutable ledger)
CREATE TABLE transactions (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID            NOT NULL REFERENCES accounts(id),
    type                    VARCHAR(30)     NOT NULL,
    amount                  NUMERIC(20, 3)  NOT NULL,
    currency_code           VARCHAR(3)      NOT NULL,
    balance_after           NUMERIC(20, 3)  NOT NULL,
    balance_after_currency  VARCHAR(3)      NOT NULL,
    description             VARCHAR(500),
    reference_transaction_id UUID           REFERENCES transactions(id),
    related_account_id      UUID            REFERENCES accounts(id),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_tx_currency_length CHECK (char_length(currency_code) = 3)
);

-- Transactions are immutable — no UPDATE or DELETE should be possible in app layer
CREATE INDEX idx_transactions_account_id        ON transactions(account_id);
CREATE INDEX idx_transactions_created_at        ON transactions(created_at DESC);
CREATE INDEX idx_transactions_reference_id      ON transactions(reference_transaction_id);
CREATE INDEX idx_transactions_related_account   ON transactions(related_account_id);
CREATE INDEX idx_transactions_type              ON transactions(type);

-- Audit trigger: prevent updates/deletes on transactions
CREATE OR REPLACE FUNCTION prevent_transaction_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Transactions are immutable and cannot be modified or deleted. TxID: %', OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transactions_immutable_update
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION prevent_transaction_modification();

CREATE TRIGGER trg_transactions_immutable_delete
    BEFORE DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION prevent_transaction_modification();
