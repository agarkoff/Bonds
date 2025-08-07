--liquibase formatted sql

--changeset bonds:1
CREATE TABLE bonds (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL UNIQUE,
    ticker VARCHAR(12),
    short_name VARCHAR(255),
    coupon_value DECIMAL(15,8),
    maturity_date DATE,
    face_value DECIMAL(15,8),
    coupon_frequency INTEGER,
    coupon_length INTEGER,
    coupon_days_passed INTEGER,
    
    -- T-Bank данные
    figi VARCHAR(50),
    instrument_uid VARCHAR(50),
    asset_uid VARCHAR(50),
    brand_name VARCHAR(255),
    
    -- Рыночные данные
    price DECIMAL(15,8),
    
    -- Рейтинги
    rating_value VARCHAR(10),
    rating_code INTEGER,
    
    -- Расчетные показатели
    coupon_daily DECIMAL(15,8),
    nkd DECIMAL(15,8),
    costs DECIMAL(15,8),
    fee DECIMAL(15,8),
    coupon_redemption DECIMAL(15,8),
    profit DECIMAL(15,8),
    profit_net DECIMAL(15,8),
    annual_yield DECIMAL(15,8),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bonds_isin ON bonds(isin);
CREATE INDEX idx_bonds_annual_yield ON bonds(annual_yield DESC);
CREATE INDEX idx_bonds_maturity_date ON bonds(maturity_date);

--changeset bonds:2
CREATE TABLE ratings (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL,
    company_name VARCHAR(255),
    rating_value VARCHAR(10) NOT NULL,
    rating_code INTEGER,
    rating_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ratings_bond FOREIGN KEY (isin) REFERENCES bonds(isin)
);

CREATE INDEX idx_ratings_isin ON ratings(isin);
CREATE INDEX idx_ratings_date ON ratings(rating_date DESC);
CREATE UNIQUE INDEX idx_ratings_unique ON ratings(isin, rating_date);
