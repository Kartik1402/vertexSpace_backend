-- ============================================================================
-- PRE-DEPLOY DATA CHECKS (Run manually in pgAdmin before production rollout)
-- Purpose: detect data issues that can break migrations or create ambiguity.
-- ============================================================================

-- 1) Required department codes for seed mappings
SELECT code, name, is_active
FROM departments
WHERE code IN ('ENG', 'HR', 'IT', 'SALES', 'MARKETING')
ORDER BY code;

-- 2) Missing required department codes
SELECT req.code AS missing_department_code
FROM (
    VALUES ('ENG'), ('HR'), ('IT'), ('SALES'), ('MARKETING')
) AS req(code)
LEFT JOIN departments d ON d.code = req.code
WHERE d.id IS NULL;

-- 3) Duplicate department codes (must be 0 rows)
SELECT code, COUNT(*) AS duplicate_count
FROM departments
GROUP BY code
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, code;

-- 4) Duplicate buildings by natural business key (must be 0 rows)
SELECT
    name,
    address,
    city,
    state,
    country,
    COUNT(*) AS duplicate_count
FROM buildings
GROUP BY name, address, city, state, country
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, name;

-- 5) Duplicate floors per building/floor_number (must be 0 rows)
SELECT
    f.building_id,
    b.name AS building_name,
    f.floor_number,
    COUNT(*) AS duplicate_count
FROM floors f
JOIN buildings b ON b.id = f.building_id
GROUP BY f.building_id, b.name, f.floor_number
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, b.name, f.floor_number;

-- 6) Duplicate resources per floor/name (must be 0 rows)
SELECT
    r.floor_id,
    f.floor_number,
    b.name AS building_name,
    r.name AS resource_name,
    COUNT(*) AS duplicate_count
FROM resources r
JOIN floors f ON f.id = r.floor_id
JOIN buildings b ON b.id = f.building_id
GROUP BY r.floor_id, f.floor_number, b.name, r.name
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, b.name, f.floor_number, r.name;

-- 7) Optional health summary counts
SELECT
    (SELECT COUNT(*) FROM departments) AS departments_total,
    (SELECT COUNT(*) FROM buildings) AS buildings_total,
    (SELECT COUNT(*) FROM floors) AS floors_total,
    (SELECT COUNT(*) FROM resources) AS resources_total;
