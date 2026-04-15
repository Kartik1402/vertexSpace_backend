-- ============================================================================
-- Seed admin users for protected APIs (idempotent)
-- Creates:
-- - 1 SYSTEM_ADMIN
-- - 3 DEPT_ADMIN users
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    dept_it_id UUID;
    dept_eng_id UUID;
    dept_hr_id UUID;
    dept_sales_id UUID;
BEGIN
    SELECT id INTO dept_it_id FROM departments WHERE code = 'IT' LIMIT 1;
    SELECT id INTO dept_eng_id FROM departments WHERE code = 'ENG' LIMIT 1;
    SELECT id INTO dept_hr_id FROM departments WHERE code = 'HR' LIMIT 1;
    SELECT id INTO dept_sales_id FROM departments WHERE code = 'SALES' LIMIT 1;

    IF dept_it_id IS NULL THEN
        RAISE EXCEPTION 'Department code IT not found. Cannot seed system admin user.';
    END IF;

    IF dept_eng_id IS NULL THEN
        RAISE EXCEPTION 'Department code ENG not found. Cannot seed ENG department admin user.';
    END IF;

    IF dept_hr_id IS NULL THEN
        RAISE EXCEPTION 'Department code HR not found. Cannot seed HR department admin user.';
    END IF;

    IF dept_sales_id IS NULL THEN
        RAISE EXCEPTION 'Department code SALES not found. Cannot seed SALES department admin user.';
    END IF;

    INSERT INTO users (
        id,
        email,
        password_hash,
        display_name,
        role,
        department_id,
        is_active,
        created_at_utc
    )
    VALUES (
        gen_random_uuid(),
        'sysadmin@vertexspace.local',
        crypt('SysAdmin@123', gen_salt('bf', 10)),
        'System Admin',
        'SYSTEM_ADMIN',
        dept_it_id,
        TRUE,
        NOW()
    )
    ON CONFLICT (email)
    DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        department_id = EXCLUDED.department_id,
        is_active = TRUE;

    INSERT INTO users (
        id,
        email,
        password_hash,
        display_name,
        role,
        department_id,
        is_active,
        created_at_utc
    )
    VALUES (
        gen_random_uuid(),
        'eng.admin@vertexspace.local',
        crypt('EngAdmin@123', gen_salt('bf', 10)),
        'Engineering Admin',
        'DEPT_ADMIN',
        dept_eng_id,
        TRUE,
        NOW()
    )
    ON CONFLICT (email)
    DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        department_id = EXCLUDED.department_id,
        is_active = TRUE;

    INSERT INTO users (
        id,
        email,
        password_hash,
        display_name,
        role,
        department_id,
        is_active,
        created_at_utc
    )
    VALUES (
        gen_random_uuid(),
        'hr.admin@vertexspace.local',
        crypt('HrAdmin@123', gen_salt('bf', 10)),
        'HR Admin',
        'DEPT_ADMIN',
        dept_hr_id,
        TRUE,
        NOW()
    )
    ON CONFLICT (email)
    DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        department_id = EXCLUDED.department_id,
        is_active = TRUE;

    INSERT INTO users (
        id,
        email,
        password_hash,
        display_name,
        role,
        department_id,
        is_active,
        created_at_utc
    )
    VALUES (
        gen_random_uuid(),
        'sales.admin@vertexspace.local',
        crypt('SalesAdmin@123', gen_salt('bf', 10)),
        'Sales Admin',
        'DEPT_ADMIN',
        dept_sales_id,
        TRUE,
        NOW()
    )
    ON CONFLICT (email)
    DO UPDATE SET
        password_hash = EXCLUDED.password_hash,
        display_name = EXCLUDED.display_name,
        role = EXCLUDED.role,
        department_id = EXCLUDED.department_id,
        is_active = TRUE;
END $$;
