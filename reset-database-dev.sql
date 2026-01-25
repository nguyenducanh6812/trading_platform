-- ===================================================================
-- DEVELOPMENT ONLY: Reset Database Script
-- This script drops and recreates the trading_platform schema
-- Use this during development when you modify existing changesets
-- ===================================================================

-- Drop the schema cascade (removes all tables, views, sequences, etc.)
DROP SCHEMA IF EXISTS trading_platform CASCADE;

-- Recreate the schema
CREATE SCHEMA trading_platform;

-- Grant privileges to trading_user
GRANT ALL PRIVILEGES ON SCHEMA trading_platform TO trading_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA trading_platform TO trading_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA trading_platform TO trading_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA trading_platform
GRANT ALL PRIVILEGES ON TABLES TO trading_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA trading_platform
GRANT ALL PRIVILEGES ON SEQUENCES TO trading_user;

-- Success message
SELECT 'Database reset successfully. You can now restart the application.' AS status;
