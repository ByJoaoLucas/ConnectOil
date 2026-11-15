-- Database initialization script for Oil Collection System
-- This script runs when PostgreSQL container starts for the first time

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create database if not exists (handled by POSTGRES_DB env var)

-- Set timezone
SET TIMEZONE = 'America/Sao_Paulo';

-- Create application user if not exists (optional, for production)
-- DO $$
-- BEGIN
--     IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'oil_collection_app') THEN
--         CREATE ROLE oil_collection_app WITH LOGIN PASSWORD 'secure_password';
--     END IF;
-- END
-- $$;

-- Grant permissions
-- GRANT ALL PRIVILEGES ON DATABASE oil_collection TO oil_collection_app;
-- GRANT ALL ON SCHEMA public TO oil_collection_app;

-- The actual schema will be created by Flyway migrations on application startup
-- This file is just for initial database setup

SELECT 'Oil Collection Database initialized' AS status;