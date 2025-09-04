-- Market Data Module Database Schema
-- This schema will be automatically created by Hibernate ddl-auto=update
-- This file serves as documentation of the expected database structure

-- Market Instruments Table
CREATE TABLE IF NOT EXISTS market_instruments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    base_currency VARCHAR(10) NOT NULL,
    quote_currency VARCHAR(10) NOT NULL,
    data_point_count INTEGER NOT NULL DEFAULT 0,
    quality_score DECIMAL(5,2),
    quality_level VARCHAR(20),
    data_source VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

-- Create indexes for market_instruments
CREATE INDEX IF NOT EXISTS idx_market_instrument_symbol ON market_instruments(symbol);
CREATE INDEX IF NOT EXISTS idx_market_instrument_base_currency ON market_instruments(base_currency);
CREATE INDEX IF NOT EXISTS idx_market_instrument_quote_currency ON market_instruments(quote_currency);

-- Bitcoin Price Data Table (Asset-specific storage)
CREATE TABLE IF NOT EXISTS btc_price_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL UNIQUE,
    open_price DECIMAL(18,8) NOT NULL,
    high_price DECIMAL(18,8) NOT NULL,
    low_price DECIMAL(18,8) NOT NULL,
    close_price DECIMAL(18,8) NOT NULL,
    volume DECIMAL(24,8) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for BTC price data
CREATE INDEX IF NOT EXISTS idx_btc_timestamp ON btc_price_data(timestamp);
CREATE INDEX IF NOT EXISTS idx_btc_timestamp_desc ON btc_price_data(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_btc_close_price ON btc_price_data(close_price);

-- Ethereum Price Data Table (Asset-specific storage)
CREATE TABLE IF NOT EXISTS eth_price_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL UNIQUE,
    open_price DECIMAL(18,8) NOT NULL,
    high_price DECIMAL(18,8) NOT NULL,
    low_price DECIMAL(18,8) NOT NULL,
    close_price DECIMAL(18,8) NOT NULL,
    volume DECIMAL(24,8) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for ETH price data
CREATE INDEX IF NOT EXISTS idx_eth_timestamp ON eth_price_data(timestamp);
CREATE INDEX IF NOT EXISTS idx_eth_timestamp_desc ON eth_price_data(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_eth_close_price ON eth_price_data(close_price);