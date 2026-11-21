-- Migration V6: Add establishment_id to collection_points and make collector_id nullable in collection_schedules
-- Version: 6.0.0

-- ============================================
-- COLLECTION SCHEDULES: Make collector_id nullable (US02)
-- ============================================
ALTER TABLE collection_schedules ALTER COLUMN collector_id DROP NOT NULL;

-- ============================================
-- COLLECTION POINTS: Add establishment_id FK to users (US02)
-- ============================================
ALTER TABLE collection_points ADD COLUMN IF NOT EXISTS establishment_id UUID REFERENCES users(id);

-- Add index for establishment_id
CREATE INDEX IF NOT EXISTS idx_point_establishment ON collection_points(establishment_id);

-- ============================================
-- Add index for schedule time_window (US02)
-- ============================================
CREATE INDEX IF NOT EXISTS idx_schedule_time_window ON collection_schedules(time_window);

-- ============================================
-- Add index for schedule sale_type (already exists from V2, but ensure it's there)
-- ============================================
-- Already exists: CREATE INDEX IF NOT EXISTS idx_schedule_sale_type ON collection_schedules(sale_type);