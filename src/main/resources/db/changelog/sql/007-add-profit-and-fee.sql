--liquibase formatted sql

--changeset author:add-profit-and-fee-7 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN fee DECIMAL(20, 4);
ALTER TABLE bonds ADD COLUMN profit DECIMAL(20, 4);

CREATE INDEX idx_bonds_fee ON bonds(fee);
CREATE INDEX idx_bonds_profit ON bonds(profit);

--rollback ALTER TABLE bonds DROP COLUMN fee, DROP COLUMN profit;