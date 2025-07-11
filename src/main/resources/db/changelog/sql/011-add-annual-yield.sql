--liquibase formatted sql

--changeset author:add-annual-yield-11 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN annual_yield DECIMAL(20, 4) NOT NULL DEFAULT 0;

CREATE INDEX idx_bonds_annual_yield ON bonds(annual_yield);

--rollback ALTER TABLE bonds DROP COLUMN annual_yield;