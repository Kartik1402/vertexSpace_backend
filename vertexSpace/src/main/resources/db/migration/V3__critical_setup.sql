-- ============================================================================
-- CRITICAL SETUP FOR MILESTONE 2 (Hybrid Approach)
-- Tables auto-created by Hibernate (ddl-auto=update)
-- This migration adds: exclusion constraint + seed data + indexes
-- ============================================================================

-- Enable btree_gist extension (required for exclusion constraint)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ============================================================================
-- CRITICAL: Exclusion Constraint (Prevents Double Bookings!)
-- ============================================================================

ALTER TABLE resource_time_blocks
    ADD CONSTRAINT no_overlap_active_blocks
    EXCLUDE USING GIST (
    resource_id WITH =,
    tstzrange(start_time_utc, conflict_end_utc) WITH &&
)
WHERE (status IN ('CONFIRMED', 'OFFERED'));

-- What this does:
-- 1. Prevents two active blocks from overlapping on the same resource
-- 2. Uses GiST index for efficient range queries
-- 3. Only applies to CONFIRMED and OFFERED blocks (not CANCELLED/EXPIRED)
-- 4. Works at database level (cannot be bypassed by application code!)

-- ============================================================================
-- Additional Unique Constraints (if not already created by Hibernate)
-- ============================================================================

-- Ensure unique floor numbers per building
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'unique_floor_per_building'
    ) THEN
ALTER TABLE floors
    ADD CONSTRAINT unique_floor_per_building
        UNIQUE (building_id, floor_number);
END IF;
END $$;

-- Ensure unique resource names per floor
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'unique_resource_per_floor'
    ) THEN
ALTER TABLE resources
    ADD CONSTRAINT unique_resource_per_floor
        UNIQUE (floor_id, name);
END IF;
END $$;

-- ============================================================================
-- Performance Indexes
-- ============================================================================

-- Buildings indexes
CREATE INDEX IF NOT EXISTS idx_buildings_active ON buildings(is_active) WHERE is_active = TRUE;

-- Floors indexes
CREATE INDEX IF NOT EXISTS idx_floors_building ON floors(building_id);
CREATE INDEX IF NOT EXISTS idx_floors_active ON floors(is_active) WHERE is_active = TRUE;

-- Resources indexes
CREATE INDEX IF NOT EXISTS idx_resources_floor ON resources(floor_id);
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(resource_type);
CREATE INDEX IF NOT EXISTS idx_resources_department ON resources(owning_department_id);
CREATE INDEX IF NOT EXISTS idx_resources_active ON resources(is_active) WHERE is_active = TRUE;

-- Time blocks indexes (CRITICAL for booking queries)
CREATE INDEX IF NOT EXISTS idx_timeblocks_resource ON resource_time_blocks(resource_id);
CREATE INDEX IF NOT EXISTS idx_timeblocks_user ON resource_time_blocks(user_id);
CREATE INDEX IF NOT EXISTS idx_timeblocks_status ON resource_time_blocks(status);
CREATE INDEX IF NOT EXISTS idx_timeblocks_type ON resource_time_blocks(block_type);
CREATE INDEX IF NOT EXISTS idx_timeblocks_time_range ON resource_time_blocks(start_time_utc, end_time_utc);

-- Composite index for conflict checking
CREATE INDEX IF NOT EXISTS idx_timeblocks_conflict_check
    ON resource_time_blocks(resource_id, status, start_time_utc, conflict_end_utc);

-- ============================================================================
-- SEED DATA (Test Buildings, Floors, Resources)
-- ============================================================================

DO $$
DECLARE
    -- Building IDs
building_tower_a_id UUID := gen_random_uuid();
    building_tower_b_id UUID := gen_random_uuid();
    building_annex_id UUID := gen_random_uuid();

    -- Floor IDs
    floor_ta_1_id UUID := gen_random_uuid();
    floor_ta_2_id UUID := gen_random_uuid();
    floor_ta_3_id UUID := gen_random_uuid();
    floor_tb_1_id UUID := gen_random_uuid();
    floor_tb_2_id UUID := gen_random_uuid();
    floor_annex_gf_id UUID := gen_random_uuid();

    -- Department IDs
    dept_eng_id UUID;
    dept_hr_id UUID;
    dept_it_id UUID;
    dept_sales_id UUID;
    dept_marketing_id UUID;

BEGIN
    -- ========================================================================
    -- Get existing department IDs
    -- ========================================================================
SELECT id INTO dept_eng_id FROM departments WHERE code = 'ENG';
SELECT id INTO dept_hr_id FROM departments WHERE code = 'HR';
SELECT id INTO dept_it_id FROM departments WHERE code = 'IT';
SELECT id INTO dept_sales_id FROM departments WHERE code = 'SALES';
SELECT id INTO dept_marketing_id FROM departments WHERE code = 'MARKETING';

-- ========================================================================
-- Insert Buildings
-- ========================================================================
INSERT INTO buildings (id, name, address, city, state, zip_code, country, is_active, created_at_utc) VALUES
                                                                                                         (building_tower_a_id, 'Tower A', '123 Main Street', 'New York', 'NY', '10001', 'USA', TRUE, NOW()),
                                                                                                         (building_tower_b_id, 'Tower B', '125 Main Street', 'New York', 'NY', '10001', 'USA', TRUE, NOW()),
                                                                                                         (building_annex_id, 'Annex Building', '200 Park Avenue', 'New York', 'NY', '10002', 'USA', TRUE, NOW());

-- ========================================================================
-- Insert Floors
-- ========================================================================
INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc) VALUES
                                                                                              -- Tower A
                                                                                              (floor_ta_1_id, building_tower_a_id, 1, '1st Floor', TRUE, NOW()),
                                                                                              (floor_ta_2_id, building_tower_a_id, 2, '2nd Floor', TRUE, NOW()),
                                                                                              (floor_ta_3_id, building_tower_a_id, 3, '3rd Floor', TRUE, NOW()),

                                                                                              -- Tower B
                                                                                              (floor_tb_1_id, building_tower_b_id, 1, '1st Floor', TRUE, NOW()),
                                                                                              (floor_tb_2_id, building_tower_b_id, 2, '2nd Floor', TRUE, NOW()),

                                                                                              -- Annex
                                                                                              (floor_annex_gf_id, building_annex_id, 0, 'Ground Floor', TRUE, NOW());

-- ========================================================================
-- Insert Resources (Mix of ROOM, DESK, PARKING)
-- ========================================================================

-- Tower A - 1st Floor (Engineering)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_ta_1_id, dept_eng_id, 'ROOM', 'Conference Room A101', 10, 'Large conference room with projector', TRUE, NOW()),
                                                                                                                                  (floor_ta_1_id, dept_eng_id, 'ROOM', 'Conference Room A102', 6, 'Small meeting room', TRUE, NOW()),
                                                                                                                                  (floor_ta_1_id, dept_eng_id, 'DESK', 'Desk A-101', NULL, 'Hot desk near window', TRUE, NOW()),
                                                                                                                                  (floor_ta_1_id, dept_eng_id, 'DESK', 'Desk A-102', NULL, 'Hot desk in quiet zone', TRUE, NOW()),
                                                                                                                                  (floor_ta_1_id, dept_eng_id, 'DESK', 'Desk A-103', NULL, 'Hot desk with dual monitors', TRUE, NOW());

-- Tower A - 2nd Floor (HR)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_ta_2_id, dept_hr_id, 'ROOM', 'Interview Room A201', 4, 'Private interview room', TRUE, NOW()),
                                                                                                                                  (floor_ta_2_id, dept_hr_id, 'ROOM', 'Training Room A202', 20, 'Large training room with whiteboard', TRUE, NOW()),
                                                                                                                                  (floor_ta_2_id, dept_hr_id, 'DESK', 'Desk A-201', NULL, 'Hot desk for visitors', TRUE, NOW());

-- Tower A - 3rd Floor (IT)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_ta_3_id, dept_it_id, 'ROOM', 'Server Room A301', 4, 'Restricted access server room', TRUE, NOW()),
                                                                                                                                  (floor_ta_3_id, dept_it_id, 'DESK', 'Desk A-301', NULL, 'Hot desk with extra power outlets', TRUE, NOW());

-- Tower B - 1st Floor (Sales)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_tb_1_id, dept_sales_id, 'ROOM', 'Conference Room B101', 8, 'Client meeting room', TRUE, NOW()),
                                                                                                                                  (floor_tb_1_id, dept_sales_id, 'DESK', 'Desk B-101', NULL, 'Hot desk in sales area', TRUE, NOW()),
                                                                                                                                  (floor_tb_1_id, dept_sales_id, 'DESK', 'Desk B-102', NULL, 'Hot desk near coffee station', TRUE, NOW());

-- Tower B - 2nd Floor (Marketing)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_tb_2_id, dept_marketing_id, 'ROOM', 'Creative Studio B201', 12, 'Open studio with design tools', TRUE, NOW()),
                                                                                                                                  (floor_tb_2_id, dept_marketing_id, 'DESK', 'Desk B-201', NULL, 'Collaboration desk', TRUE, NOW());

-- Annex - Ground Floor (Parking)
INSERT INTO resources (floor_id, owning_department_id, resource_type, name, capacity, description, is_active, created_at_utc) VALUES
                                                                                                                                  (floor_annex_gf_id, dept_eng_id, 'PARKING', 'Parking Spot P1', NULL, 'Covered parking near entrance', TRUE, NOW()),
                                                                                                                                  (floor_annex_gf_id, dept_eng_id, 'PARKING', 'Parking Spot P2', NULL, 'Covered parking', TRUE, NOW()),
                                                                                                                                  (floor_annex_gf_id, dept_eng_id, 'PARKING', 'Parking Spot P3', NULL, 'Open parking', TRUE, NOW()),
                                                                                                                                  (floor_annex_gf_id, dept_eng_id, 'PARKING', 'Parking Spot P4', NULL, 'Open parking', TRUE, NOW());

END $$;

-- ============================================================================
-- Verification Queries (for logging)
-- ============================================================================

DO $$
DECLARE
building_count INT;
    floor_count INT;
    resource_count INT;
BEGIN
SELECT COUNT(*) INTO building_count FROM buildings;
SELECT COUNT(*) INTO floor_count FROM floors;
SELECT COUNT(*) INTO resource_count FROM resources;

RAISE NOTICE 'Seed data inserted:';
    RAISE NOTICE '  - Buildings: %', building_count;
    RAISE NOTICE '  - Floors: %', floor_count;
    RAISE NOTICE '  - Resources: %', resource_count;
END $$;

-- ============================================================================
-- Add Comments (Documentation)
-- ============================================================================

COMMENT ON CONSTRAINT no_overlap_active_blocks ON resource_time_blocks IS
'Prevents overlapping bookings/offers on same resource. Uses conflict_end_utc (end + buffer) for range check.';

COMMENT ON COLUMN resource_time_blocks.conflict_end_utc IS
'Computed field: end_time_utc + buffer_minutes. Used in exclusion constraint.';

COMMENT ON COLUMN resource_time_blocks.block_type IS
'BOOKING = regular booking | OFFER_HOLD = waitlist offer (15-min countdown)';

COMMENT ON COLUMN resource_time_blocks.status IS
'CONFIRMED = active booking | OFFERED = pending acceptance | OFFER_EXPIRED = user did not accept | CANCELLED = booking cancelled';

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
