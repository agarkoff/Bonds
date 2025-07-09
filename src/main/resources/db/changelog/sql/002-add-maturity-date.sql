--liquibase formatted sql

--changeset author:add-maturity-date-2 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN maturity_date DATE;

CREATE INDEX idx_bonds_maturity_date ON bonds(maturity_date);

--rollback ALTER TABLE bonds DROP COLUMN maturity_date;