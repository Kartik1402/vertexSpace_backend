-- ============================================================================
-- Enforce business-key uniqueness for buildings
-- Safe strategy:
-- 1) Detect duplicates and fail with an explicit message
-- 2) Add unique constraint only when data is clean
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT name, address, city, state, country, COUNT(*)
            FROM buildings
            GROUP BY name, address, city, state, country
            HAVING COUNT(*) > 1
        ) dups
    ) THEN
        RAISE EXCEPTION 'Cannot add unique_building_business_key: duplicate building records exist for (name, address, city, state, country). Clean duplicates first and rerun migration.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'unique_building_business_key'
    ) THEN
        ALTER TABLE buildings
            ADD CONSTRAINT unique_building_business_key
            UNIQUE (name, address, city, state, country);
    END IF;
END $$;
