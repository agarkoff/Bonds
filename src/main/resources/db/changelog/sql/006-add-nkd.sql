--liquibase formatted sql

--changeset author:add-nkd-6 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN nkd DECIMAL(20, 4);

CREATE INDEX idx_bonds_nkd ON bonds(nkd);

--rollback ALTER TABLE bonds DROP COLUMN nkd;