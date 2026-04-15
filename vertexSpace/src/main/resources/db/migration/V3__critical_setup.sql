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
BEGIN
    -- Ensure required departments exist (idempotent for existing production data)
    INSERT INTO departments (id, code, name, is_active, created_at_utc)
    VALUES
        (gen_random_uuid(), 'ENG', 'Engineering', TRUE, NOW()),
        (gen_random_uuid(), 'HR', 'Human Resources', TRUE, NOW()),
        (gen_random_uuid(), 'IT', 'Information Technology', TRUE, NOW()),
        (gen_random_uuid(), 'SALES', 'Sales', TRUE, NOW()),
        (gen_random_uuid(), 'MARKETING', 'Marketing', TRUE, NOW())
    ON CONFLICT (code)
    DO UPDATE SET
        name = EXCLUDED.name,
        is_active = TRUE;

    -- Insert buildings only when missing
    INSERT INTO buildings (id, name, address, city, state, zip_code, country, is_active, created_at_utc)
    SELECT gen_random_uuid(), 'Tower A', '123 Main Street', 'New York', 'NY', '10001', 'USA', TRUE, NOW()
    WHERE NOT EXISTS (
        SELECT 1 FROM buildings b WHERE b.name = 'Tower A' AND b.address = '123 Main Street'
    );

    INSERT INTO buildings (id, name, address, city, state, zip_code, country, is_active, created_at_utc)
    SELECT gen_random_uuid(), 'Tower B', '125 Main Street', 'New York', 'NY', '10001', 'USA', TRUE, NOW()
    WHERE NOT EXISTS (
        SELECT 1 FROM buildings b WHERE b.name = 'Tower B' AND b.address = '125 Main Street'
    );

    INSERT INTO buildings (id, name, address, city, state, zip_code, country, is_active, created_at_utc)
    SELECT gen_random_uuid(), 'Annex Building', '200 Park Avenue', 'New York', 'NY', '10002', 'USA', TRUE, NOW()
    WHERE NOT EXISTS (
        SELECT 1 FROM buildings b WHERE b.name = 'Annex Building' AND b.address = '200 Park Avenue'
    );

    -- Insert floors only when missing
    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 1, '1st Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Tower A'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 1
    );

    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 2, '2nd Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Tower A'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 2
    );

    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 3, '3rd Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Tower A'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 3
    );

    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 1, '1st Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Tower B'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 1
    );

    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 2, '2nd Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Tower B'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 2
    );

    INSERT INTO floors (id, building_id, floor_number, floor_name, is_active, created_at_utc)
    SELECT gen_random_uuid(), b.id, 0, 'Ground Floor', TRUE, NOW()
    FROM buildings b
    WHERE b.name = 'Annex Building'
      AND NOT EXISTS (
        SELECT 1 FROM floors f WHERE f.building_id = b.id AND f.floor_number = 0
    );

    -- Insert resources only when missing for a floor
    INSERT INTO resources (
        id,
        floor_id,
        owning_department_id,
        resource_type,
        assignment_mode,
        name,
        capacity,
        description,
        is_active,
        created_at_utc
    )
    SELECT
        gen_random_uuid(),
        f.id,
        d.id,
        x.resource_type,
        x.assignment_mode,
        x.resource_name,
        x.capacity,
        x.description,
        TRUE,
        NOW()
    FROM (
        VALUES
            ('Tower A', 1, 'ENG', 'ROOM', 'NOT_APPLICABLE', 'Conference Room A101', 10, 'Large conference room with projector'),
            ('Tower A', 1, 'ENG', 'ROOM', 'NOT_APPLICABLE', 'Conference Room A102', 6, 'Small meeting room'),
            ('Tower A', 1, 'ENG', 'DESK', 'HOT_DESK', 'Desk A-101', NULL, 'Hot desk near window'),
            ('Tower A', 1, 'ENG', 'DESK', 'HOT_DESK', 'Desk A-102', NULL, 'Hot desk in quiet zone'),
            ('Tower A', 1, 'ENG', 'DESK', 'HOT_DESK', 'Desk A-103', NULL, 'Hot desk with dual monitors'),
            ('Tower A', 2, 'HR', 'ROOM', 'NOT_APPLICABLE', 'Interview Room A201', 4, 'Private interview room'),
            ('Tower A', 2, 'HR', 'ROOM', 'NOT_APPLICABLE', 'Training Room A202', 20, 'Large training room with whiteboard'),
            ('Tower A', 2, 'HR', 'DESK', 'HOT_DESK', 'Desk A-201', NULL, 'Hot desk for visitors'),
            ('Tower A', 3, 'IT', 'ROOM', 'NOT_APPLICABLE', 'Server Room A301', 4, 'Restricted access server room'),
            ('Tower A', 3, 'IT', 'DESK', 'HOT_DESK', 'Desk A-301', NULL, 'Hot desk with extra power outlets'),
            ('Tower B', 1, 'SALES', 'ROOM', 'NOT_APPLICABLE', 'Conference Room B101', 8, 'Client meeting room'),
            ('Tower B', 1, 'SALES', 'DESK', 'HOT_DESK', 'Desk B-101', NULL, 'Hot desk in sales area'),
            ('Tower B', 1, 'SALES', 'DESK', 'HOT_DESK', 'Desk B-102', NULL, 'Hot desk near coffee station'),
            ('Tower B', 2, 'MARKETING', 'ROOM', 'NOT_APPLICABLE', 'Creative Studio B201', 12, 'Open studio with design tools'),
            ('Tower B', 2, 'MARKETING', 'DESK', 'HOT_DESK', 'Desk B-201', NULL, 'Collaboration desk'),
            ('Annex Building', 0, 'ENG', 'PARKING', 'NOT_APPLICABLE', 'Parking Spot P1', NULL, 'Covered parking near entrance'),
            ('Annex Building', 0, 'ENG', 'PARKING', 'NOT_APPLICABLE', 'Parking Spot P2', NULL, 'Covered parking'),
            ('Annex Building', 0, 'ENG', 'PARKING', 'NOT_APPLICABLE', 'Parking Spot P3', NULL, 'Open parking'),
            ('Annex Building', 0, 'ENG', 'PARKING', 'NOT_APPLICABLE', 'Parking Spot P4', NULL, 'Open parking')
    ) AS x(building_name, floor_number, department_code, resource_type, assignment_mode, resource_name, capacity, description)
    JOIN buildings b ON b.name = x.building_name
    JOIN floors f ON f.building_id = b.id AND f.floor_number = x.floor_number
    JOIN departments d ON d.code = x.department_code
    WHERE NOT EXISTS (
        SELECT 1
        FROM resources r
        WHERE r.floor_id = f.id
          AND r.name = x.resource_name
    );

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
