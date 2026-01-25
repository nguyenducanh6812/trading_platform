-- ========================================
-- Trading Platform - Database Initialization
-- ========================================
-- This script initializes both databases and their schemas
-- Run this ONCE before the first application startup
--
-- For Docker: Automatically executed via docker-entrypoint-initdb.d
-- For Production: Run manually by DBA or deployment automation
--
-- Creates:
-- 1. camunda database - for Camunda workflow engine
-- 2. trading_platform schema - for business data (in trading_platform DB)
-- ========================================

-- ========================================
-- 1. Create Camunda Database
-- ========================================

-- Create camunda database if it doesn't exist
SELECT 'CREATE DATABASE camunda'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'camunda')\gexec

-- Grant privileges to trading_user on camunda database
GRANT ALL PRIVILEGES ON DATABASE camunda TO trading_user;

-- Switch to camunda database to set default privileges
\c camunda

-- Grant usage and create on public schema
GRANT ALL ON SCHEMA public TO trading_user;

-- Set default privileges for future tables and sequences in camunda database
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO trading_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO trading_user;

DO $$
BEGIN
    RAISE NOTICE '✓ Camunda database initialized successfully';
END $$;

-- ========================================
-- 2. Setup Trading Platform Database
-- ========================================

-- Switch back to trading_platform database (created by POSTGRES_DB env var)
\c trading_platform

-- Create schema if not exists (idempotent)
CREATE SCHEMA IF NOT EXISTS trading_platform;

-- Grant all privileges on schema to application user
GRANT ALL ON SCHEMA trading_platform TO trading_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA trading_platform GRANT ALL ON TABLES TO trading_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA trading_platform GRANT ALL ON SEQUENCES TO trading_user;

DO $$
BEGIN
    RAISE NOTICE '✓ Trading platform schema initialized successfully';
END $$;
