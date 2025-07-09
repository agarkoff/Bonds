--liquibase formatted sql

--changeset author:add-coupon-frequency-5 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN coupon_frequency INTEGER;

CREATE INDEX idx_bonds_coupon_frequency ON bonds(coupon_frequency);

--rollback ALTER TABLE bonds DROP COLUMN coupon_frequency;