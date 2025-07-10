--liquibase formatted sql

--changeset author:add-coupon-length-9 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN coupon_length INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_bonds_coupon_length ON bonds(coupon_length);

--rollback ALTER TABLE bonds DROP COLUMN coupon_length;