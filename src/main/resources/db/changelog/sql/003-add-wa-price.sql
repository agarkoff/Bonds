--liquibase formatted sql

--changeset author:add-wa-price-3 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN wa_price DECIMAL(20, 4);

CREATE INDEX idx_bonds_wa_price ON bonds(wa_price);

--rollback ALTER TABLE bonds DROP COLUMN wa_price;