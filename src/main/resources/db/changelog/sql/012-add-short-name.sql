--liquibase formatted sql

--changeset author:add-short-name-12 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN short_name VARCHAR(255) NOT NULL DEFAULT '';

CREATE INDEX idx_bonds_short_name ON bonds(short_name);

--rollback ALTER TABLE bonds DROP COLUMN short_name;