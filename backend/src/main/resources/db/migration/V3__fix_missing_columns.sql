-- Migration V3: Fix missing columns that were added to V2 after it was already applied
-- Version: 3.0.0

-- ============================================
-- FIX INCIDENTS TABLE: Add missing updated_at column
-- ============================================
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- ============================================
-- FIX COLLECTION_RECORDS TABLE: Add missing columns from V2
-- ============================================
-- These columns were added to V2 migration but V2 was already applied
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS establishment_confirmed_by UUID REFERENCES users(id);
ALTER TABLE collection_records ADD COLUMN IF NOT EXISTS verified_by UUID REFERENCES users(id);

-- Add indexes for the new columns
CREATE INDEX IF NOT EXISTS idx_record_establishment_confirmed_by ON collection_records(establishment_confirmed_by);
CREATE INDEX IF NOT EXISTS idx_record_verified_by ON collection_records(verified_by);