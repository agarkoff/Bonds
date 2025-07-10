--liquibase formatted sql

--changeset author:add-not-null-constraints-8 splitStatements:true endDelimiter:;
ALTER TABLE bonds ALTER COLUMN coupon_value SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN maturity_date SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN wa_price SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN face_value SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN coupon_frequency SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN nkd SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN fee SET NOT NULL;
ALTER TABLE bonds ALTER COLUMN profit SET NOT NULL;

--rollback ALTER TABLE bonds ALTER COLUMN coupon_value DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN maturity_date DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN wa_price DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN face_value DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN coupon_frequency DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN nkd DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN fee DROP NOT NULL;
--rollback ALTER TABLE bonds ALTER COLUMN profit DROP NOT NULL;