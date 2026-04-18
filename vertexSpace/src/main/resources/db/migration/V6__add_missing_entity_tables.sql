-- ============================================================================
-- Add missing entity tables for deployments where Hibernate no longer manages
-- schema evolution before Flyway validation.
-- ============================================================================

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

CREATE INDEX IF NOT EXISTS idx_desk_assignments_resource ON desk_assignments(resource_id);
CREATE INDEX IF NOT EXISTS idx_desk_assignments_user ON desk_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_desk_assignments_dates ON desk_assignments(start_utc, end_utc);

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

CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

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

CREATE INDEX IF NOT EXISTS idx_waitlist_fifo ON waitlist_entries(resource_id, start_utc, created_at);
CREATE INDEX IF NOT EXISTS idx_waitlist_user ON waitlist_entries(user_id, status);
