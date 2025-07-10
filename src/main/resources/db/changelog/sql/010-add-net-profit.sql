--liquibase formatted sql

--changeset author:add-net-profit-10 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN net_profit DECIMAL(20, 4) NOT NULL DEFAULT 0;

CREATE INDEX idx_bonds_net_profit ON bonds(net_profit);

--rollback ALTER TABLE bonds DROP COLUMN net_profit;