--liquibase formatted sql

--changeset author:add-face-value-4 splitStatements:true endDelimiter:;
ALTER TABLE bonds ADD COLUMN face_value DECIMAL(20, 4);

CREATE INDEX idx_bonds_face_value ON bonds(face_value);

--rollback ALTER TABLE bonds DROP COLUMN face_value;