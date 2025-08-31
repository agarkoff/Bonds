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

--changeset bonds:3
ALTER TABLE bonds ADD COLUMN offer_date DATE;

--changeset bonds:4
ALTER TABLE bonds ADD COLUMN coupon_offer DECIMAL(15,8);
ALTER TABLE bonds ADD COLUMN profit_offer DECIMAL(15,8);
ALTER TABLE bonds ADD COLUMN profit_net_offer DECIMAL(15,8);
ALTER TABLE bonds ADD COLUMN annual_yield_offer DECIMAL(8,4);

--changeset bonds:5
CREATE TABLE offer_subscription (
    id SERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    username VARCHAR(255),
    isin VARCHAR(12) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_offer_subscription_bond FOREIGN KEY (isin) REFERENCES bonds(isin),
    CONSTRAINT unique_subscription UNIQUE(chat_id, isin)
);

CREATE INDEX idx_offer_subscription_chat_id ON offer_subscription(chat_id);
CREATE INDEX idx_offer_subscription_isin ON offer_subscription(isin);

--changeset bonds:8
CREATE TABLE telegram_users (
    id SERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    photo_url TEXT,
    auth_date BIGINT,
    hash VARCHAR(512),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_telegram_users_telegram_id ON telegram_users(telegram_id);
CREATE INDEX idx_telegram_users_username ON telegram_users(username);

--changeset bonds:9
ALTER TABLE offer_subscription ADD COLUMN telegram_user_id BIGINT;

-- Обновляем существующие записи, связываем их с telegram_users по chat_id (который равен telegram_id)
UPDATE offer_subscription 
SET telegram_user_id = (SELECT id FROM telegram_users WHERE telegram_id = offer_subscription.chat_id)
WHERE telegram_user_id IS NULL;

-- Добавляем внешний ключ
ALTER TABLE offer_subscription 
ADD CONSTRAINT fk_offer_subscription_telegram_user 
FOREIGN KEY (telegram_user_id) REFERENCES telegram_users(id);

CREATE INDEX idx_offer_subscription_telegram_user_id ON offer_subscription(telegram_user_id);

--changeset bonds:10
-- Удаляем таблицу users, так как теперь используем только Telegram авторизацию
DROP TABLE IF EXISTS users CASCADE;

--changeset bonds:11
CREATE TABLE rating_subscription (
    id SERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    period_hours INTEGER DEFAULT 24 NOT NULL,
    min_yield DECIMAL(8,4),
    max_yield DECIMAL(8,4),
    ticker_count INTEGER DEFAULT 10 NOT NULL,
    include_offer BOOLEAN DEFAULT FALSE,
    min_maturity_weeks INTEGER,
    max_maturity_weeks INTEGER,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sent_at TIMESTAMP,
    
    CONSTRAINT fk_rating_subscription_telegram_user 
    FOREIGN KEY (telegram_user_id) REFERENCES telegram_users(id) ON DELETE CASCADE,
    
    CONSTRAINT chk_rating_subscription_period CHECK (period_hours >= 1 AND period_hours <= 168),
    CONSTRAINT chk_rating_subscription_ticker_count CHECK (ticker_count >= 1 AND ticker_count <= 100),
    CONSTRAINT chk_rating_subscription_yield_range CHECK (min_yield IS NULL OR max_yield IS NULL OR min_yield <= max_yield),
    CONSTRAINT chk_rating_subscription_maturity_range CHECK (min_maturity_weeks IS NULL OR max_maturity_weeks IS NULL OR min_maturity_weeks <= max_maturity_weeks)
);

CREATE INDEX idx_rating_subscription_telegram_user_id ON rating_subscription(telegram_user_id);
CREATE INDEX idx_rating_subscription_enabled ON rating_subscription(enabled);
CREATE INDEX idx_rating_subscription_last_sent_at ON rating_subscription(last_sent_at);
CREATE INDEX idx_rating_subscription_next_send ON rating_subscription(enabled, last_sent_at, period_hours);

--changeset bonds:12
CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_persistent_logins_username ON persistent_logins(username);
CREATE INDEX IF NOT EXISTS idx_persistent_logins_last_used ON persistent_logins(last_used);

--changeset bonds:13
ALTER TABLE bonds DROP COLUMN fee;

--changeset bonds:14
ALTER TABLE rating_subscription ADD COLUMN fee_percent DECIMAL(5,2);

--changeset bonds:15
CREATE TABLE moex_bonds (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL UNIQUE,
    short_name VARCHAR(255) NOT NULL,
    coupon_value DECIMAL(15,8) NOT NULL,
    maturity_date DATE NOT NULL,
    face_value DECIMAL(15,8) NOT NULL,
    coupon_frequency INTEGER NOT NULL,
    coupon_length INTEGER NOT NULL,
    coupon_days_passed INTEGER NOT NULL,
    offer_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moex_bonds_isin ON moex_bonds(isin);
CREATE INDEX idx_moex_bonds_maturity_date ON moex_bonds(maturity_date);
CREATE INDEX idx_moex_bonds_offer_date ON moex_bonds(offer_date);


--changeset bonds:16
CREATE TABLE tbank_bonds (
    id SERIAL PRIMARY KEY,
    instrument_uid VARCHAR(255) NOT NULL UNIQUE,
    figi VARCHAR(12) NOT NULL,
    ticker VARCHAR(12) NOT NULL,
    asset_uid VARCHAR(255) NOT NULL,
    brand_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tbank_bonds_instrument_uid ON tbank_bonds(instrument_uid);
CREATE INDEX idx_tbank_bonds_figi ON tbank_bonds(figi);
CREATE INDEX idx_tbank_bonds_ticker ON tbank_bonds(ticker);


--changeset bonds:17
CREATE TABLE tbank_prices (
    id SERIAL PRIMARY KEY,
    figi VARCHAR(12) NOT NULL UNIQUE,
    price DECIMAL(15,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tbank_prices_figi ON tbank_prices(figi);

--changeset bonds:18
CREATE TABLE raexpert_ratings (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL,
    company_name VARCHAR(255),
    rating_value VARCHAR(10) NOT NULL,
    rating_code INTEGER,
    rating_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_raexpert_rating UNIQUE(isin, rating_date)
);

CREATE INDEX idx_raexpert_ratings_isin ON raexpert_ratings(isin);
CREATE INDEX idx_raexpert_ratings_date ON raexpert_ratings(rating_date DESC);

--changeset bonds:19
CREATE TABLE dohod_ratings (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL UNIQUE,
    rating_value VARCHAR(10) NOT NULL,
    rating_code INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dohod_ratings_isin ON dohod_ratings(isin);

--changeset bonds:20
-- Создание таблицы для расчетных данных облигаций
CREATE TABLE bonds_calc (
    id SERIAL PRIMARY KEY,
    isin VARCHAR(12) NOT NULL UNIQUE,
    coupon_daily DECIMAL(15,8),
    nkd DECIMAL(15,8),
    costs DECIMAL(15,8),
    coupon_redemption DECIMAL(15,8),
    profit DECIMAL(15,8),
    profit_net DECIMAL(15,8),
    annual_yield DECIMAL(15,8),
    coupon_offer DECIMAL(15,8),
    profit_offer DECIMAL(15,8),
    profit_net_offer DECIMAL(15,8),
    annual_yield_offer DECIMAL(15,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bonds_calc_isin ON bonds_calc(isin);
CREATE INDEX idx_bonds_calc_annual_yield ON bonds_calc(annual_yield DESC);

--changeset bonds:21
-- Удаление старой таблицы bonds после создания представления
DROP TABLE IF EXISTS bonds CASCADE;

--changeset bonds:22
-- Создание представления bonds с расчетными данными

CREATE VIEW bonds AS
SELECT
    -- Генерируем уникальный ID на основе ISIN
    ('x' || substr(md5(mb.isin), 1, 8))::bit(32)::bigint as id,

    -- Основные данные из moex_bonds
    mb.isin,
    COALESCE(tb.ticker, mb.isin) as ticker,
    mb.short_name,
    mb.coupon_value,
    mb.maturity_date,
    mb.face_value,
    mb.coupon_frequency,
    mb.coupon_length,
    mb.coupon_days_passed,
    mb.offer_date,

    -- Данные из tbank_bonds
    tb.figi,
    tb.instrument_uid,
    tb.asset_uid,
    tb.brand_name,

    -- Цена из tbank_prices
    tp.price,

    -- Рейтинг из dohod_ratings
    dr.rating_value,
    dr.rating_code,

    -- Расчетные поля из bond_calculations
    bc.coupon_daily,
    bc.nkd,
    bc.costs,
    bc.coupon_redemption,
    bc.profit,
    bc.profit_net,
    bc.annual_yield,
    bc.coupon_offer,
    bc.profit_offer,
    bc.profit_net_offer,
    bc.annual_yield_offer,

    -- Отдельные даты обновления исходных сущностей
    mb.updated_at as moex_updated_at,
    tb.updated_at as tbank_bonds_updated_at,
    tp.updated_at as tbank_prices_updated_at,
    dr.updated_at as dohod_ratings_updated_at,
    bc.updated_at as bonds_calc_updated_at

FROM moex_bonds mb
LEFT JOIN tbank_bonds tb ON tb.ticker = mb.isin OR tb.figi = mb.isin
LEFT JOIN tbank_prices tp ON tp.figi = tb.figi
LEFT JOIN dohod_ratings dr ON dr.isin = mb.isin
INNER JOIN bonds_calc bc ON bc.isin = mb.isin;

--changeset bonds:23
ALTER TABLE rating_subscription ADD COLUMN selected_ratings TEXT;

--changeset bonds:24
CREATE TABLE user_filter_settings (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    limit_value INTEGER DEFAULT 50,
    weeks_to_maturity VARCHAR(50) DEFAULT '0-26',
    fee_percent DECIMAL(5,4) DEFAULT 0.30,
    yield_range VARCHAR(50) DEFAULT '0-50',
    search_text TEXT,
    show_offer BOOLEAN DEFAULT FALSE,
    selected_ratings TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_filter_settings_telegram_user 
    FOREIGN KEY (user_id) REFERENCES telegram_users(id) ON DELETE CASCADE,
    
    CONSTRAINT unique_user_settings UNIQUE(user_id)
);

CREATE INDEX idx_user_filter_settings_user_id ON user_filter_settings(user_id);

--changeset bonds:25
CREATE TABLE user_orders (
    id SERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    
    -- Основные поля сделки
    purchase_date DATE NOT NULL,
    isin VARCHAR(12) NOT NULL,
    ticker VARCHAR(20),
    bond_name VARCHAR(255),
    rating VARCHAR(10),
    coupon_value DECIMAL(10,2),
    coupon_period INTEGER,
    maturity_date DATE,
    price DECIMAL(10,2) NOT NULL,
    nkd DECIMAL(10,2),
    fee_percent DECIMAL(5,2),
    
    -- Расчетные поля
    total_costs DECIMAL(12,2),
    face_value DECIMAL(10,2),
    total_coupon DECIMAL(12,2),
    total_income DECIMAL(12,2),
    net_profit DECIMAL(12,2),
    annual_yield DECIMAL(8,2),
    
    -- Служебные поля
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Индексы и ограничения
    FOREIGN KEY (telegram_user_id) REFERENCES telegram_users(id) ON DELETE CASCADE
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_user_orders_telegram_user_id ON user_orders(telegram_user_id);
CREATE INDEX idx_user_orders_purchase_date ON user_orders(purchase_date);
CREATE INDEX idx_user_orders_isin ON user_orders(isin);
