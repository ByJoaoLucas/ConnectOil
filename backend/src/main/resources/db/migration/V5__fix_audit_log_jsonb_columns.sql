-- Migration V5: Fix audit_logs JSONB columns to TEXT
-- Version: 5.0.0

-- Change JSONB columns to TEXT to avoid double serialization issues
ALTER TABLE audit_logs ALTER COLUMN old_values TYPE TEXT USING old_values::text;
ALTER TABLE audit_logs ALTER COLUMN new_values TYPE TEXT USING new_values::text;

-- Also fix collection_points JSONB columns
ALTER TABLE collection_points ALTER COLUMN operating_hours TYPE TEXT USING operating_hours::text;
ALTER TABLE collection_points ALTER COLUMN accepted_oil_types TYPE TEXT USING accepted_oil_types::text;

-- Also fix collector_coverage JSONB column
ALTER TABLE collector_coverage ALTER COLUMN neighborhoods TYPE TEXT USING neighborhoods::text;

-- Also fix incidents JSONB column
ALTER TABLE incidents ALTER COLUMN photos TYPE TEXT USING photos::text;