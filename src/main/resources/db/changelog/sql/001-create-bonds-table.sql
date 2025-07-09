--liquibase formatted sql

--changeset author:bonds-table-1 splitStatements:true endDelimiter:;
CREATE TABLE bonds (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL UNIQUE,
    coupon_value DECIMAL(20, 4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bonds_ticker ON bonds(ticker);
CREATE INDEX idx_bonds_created_at ON bonds(created_at);

--rollback DROP TABLE bonds;