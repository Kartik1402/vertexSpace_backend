-- ============================================================================
-- CRITICAL SETUP FOR MILESTONE 2 (Hybrid Approach)
-- Tables auto-created by Hibernate (ddl-auto=update)
-- This migration adds: exclusion constraint + seed data + indexes
-- ============================================================================

-- Enable btree_gist extension (required for exclusion constraint)
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Bootstrap the core schema when deploying to a blank database.
-- Flyway runs before Hibernate in Render, so these tables must exist before
-- the ALTER TABLE and seed statements below can succeed.
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS buildings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20),
    country VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS floors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL REFERENCES buildings(id),
    floor_number INTEGER NOT NULL,
    floor_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id UUID NOT NULL REFERENCES floors(id),
    owning_department_id UUID NOT NULL REFERENCES departments(id),
    resource_type VARCHAR(20) NOT NULL,
    assignment_mode VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    department_id UUID NOT NULL REFERENCES departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ,
    last_login_utc TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS resource_time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id),
    user_id UUID NOT NULL REFERENCES users(id),
    block_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time_utc TIMESTAMPTZ NOT NULL,
    end_time_utc TIMESTAMPTZ NOT NULL,
    conflict_end_utc TIMESTAMPTZ NOT NULL,
    buffer_minutes INTEGER NOT NULL DEFAULT 15,
    purpose TEXT,
    waitlist_entry_id UUID,
    expires_at_utc TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    notes VARCHAR(500),
    created_at_utc TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at_utc TIMESTAMPTZ,
    recurring_series_id UUID,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern VARCHAR(20),
    occurrence_number INTEGER,
    original_timezone VARCHAR(50) DEFAULT 'Asia/Kolkata'
);

CREATE TABLE IF NOT EXISTS desk_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id),
    user_id UUID NOT NULL REFERENCES users(id),
    start_utc TIMESTAMPTZ NOT NULL,
    end_utc TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    assigned_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    related_entity_id UUID,
    related_entity_type VARCHAR(50),
    show_at_utc TIMESTAMPTZ,
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS notification_metadata (
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    value TEXT,
    PRIMARY KEY (notification_id, key)
);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES resources(id),
    pending_booking_id UUID REFERENCES resource_time_blocks(id),
    user_id UUID NOT NULL REFERENCES users(id),
    start_utc TIMESTAMPTZ NOT NULL,
    end_utc TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    offered_at TIMESTAMPTZ,
    fulfilled_at TIMESTAMPTZ,
    requested_start_time TIMESTAMPTZ,
    requested_end_time TIMESTAMPTZ,
    purpose TEXT,
    queue_position INTEGER,
    offer_expires_at TIMESTAMPTZ,
    CONSTRAINT unique_waitlist_slot_user UNIQUE (resource_id, start_utc, end_utc, user_id)
);

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
