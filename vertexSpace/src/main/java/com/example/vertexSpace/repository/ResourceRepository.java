package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Resource entity
 *
 * Provides queries for:
 * - Basic resource retrieval
 * - Filtering by type, location, department
 * - Availability checking (time-based conflict detection)
 * - Name uniqueness validation
 * - Statistics
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    // ========================================================================
    // BASIC QUERIES
    // ========================================================================

    /**
     * Find all active resources
     */
    List<Resource> findByIsActiveTrue();

    /**
     * Find resources by floor (active and inactive)
     */
    @Query("""
        SELECT r FROM Resource r
        WHERE r.floor.id = :floorId
        ORDER BY r.name
        """)
    List<Resource> findByFloorId(@Param("floorId") UUID floorId);

    /**
     * Find active resources by floor
     */
    @Query("""
        SELECT r FROM Resource r
        WHERE r.floor.id = :floorId
          AND r.isActive = true
        ORDER BY r.name
        """)
    List<Resource> findByFloorIdAndIsActiveTrue(@Param("floorId") UUID floorId);

    /**
     * Find resources by building (active and inactive)
     */
    @Query("""
        SELECT r FROM Resource r
        JOIN r.floor f
        WHERE f.building.id = :buildingId
        ORDER BY f.floorNumber, r.name
        """)
    List<Resource> findByBuildingId(@Param("buildingId") UUID buildingId);

    /**
     * Find active resources by building
     */
    @Query("""
        SELECT r FROM Resource r
        JOIN r.floor f
        WHERE f.building.id = :buildingId
          AND r.isActive = true
        ORDER BY f.floorNumber, r.name
        """)
    List<Resource> findByBuildingIdAndIsActiveTrue(@Param("buildingId") UUID buildingId);

    /**
     * Find active resources by type
     * Spring Data derived query - no explicit JPQL needed
     */
    List<Resource> findByResourceTypeAndIsActiveTrue(ResourceType resourceType);

    /**
     * Find active resources by department
     */
    @Query("""
        SELECT r FROM Resource r
        WHERE r.owningDepartment.id = :departmentId
          AND r.isActive = true
        ORDER BY r.name
        """)
    List<Resource> findByOwningDepartmentIdAndIsActiveTrue(@Param("departmentId") UUID departmentId);

    /**
     * Find rooms with minimum capacity
     */
    @Query("""
        SELECT r FROM Resource r
        WHERE r.resourceType = com.example.vertexSpace.enums.ResourceType.ROOM
          AND r.capacity >= :minCapacity
          AND r.isActive = true
        ORDER BY r.capacity, r.name
        """)
    List<Resource> findRoomsByMinCapacity(@Param("minCapacity") Integer minCapacity);

    /**
     * Find resource by ID with all relations eagerly loaded (avoids N+1 queries)
     */
    @Query("""
        SELECT r FROM Resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        JOIN FETCH r.owningDepartment
        WHERE r.id = :resourceId
        """)
    Optional<Resource> findByIdWithRelations(@Param("resourceId") UUID resourceId);

    // ========================================================================
    // NAME UNIQUENESS VALIDATION
    // ========================================================================

    /**
     * Check if resource name exists on floor (case-insensitive)
     * Used during resource creation
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Resource r
        WHERE r.floor.id = :floorId
          AND LOWER(r.name) = LOWER(:name)
        """)
    boolean existsByFloorIdAndNameIgnoreCase(
            @Param("floorId") UUID floorId,
            @Param("name") String name
    );

    /**
     * Check if resource name exists on floor, excluding a specific resource
     * Used during resource updates to allow keeping the same name
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Resource r
        WHERE r.floor.id = :floorId
          AND LOWER(r.name) = LOWER(:name)
          AND r.id <> :excludeId
        """)
    boolean existsByFloorIdAndNameIgnoreCaseAndIdNot(
            @Param("floorId") UUID floorId,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId
    );

    // ========================================================================
    // FILTERING (NO AVAILABILITY CHECK)
    // ========================================================================

    /**
     * Find resources by multiple filters
     *
     * Filters by resource properties only - does NOT check availability.
     * Use findAvailableByFilters() if you need time-based availability.
     *
     * All parameters are optional (null means "don't filter by this")
     *
     * @param resourceType Filter by DESK, ROOM, or PARKING_SPOT (null = all types)
     * @param floorId Filter by specific floor (null = all floors)
     * @param buildingId Filter by specific building (null = all buildings)
     * @param departmentId Filter by owning department (null = all departments)
     * @param capacityMin Minimum capacity (only applies to ROOM type)
     * @return List of matching active resources, sorted by name
     */
    // Code Generated by Sidekick is for learning and experimentation purposes only.
    @Query("""
    SELECT r FROM Resource r
    JOIN FETCH r.floor f
    JOIN FETCH f.building b
    JOIN FETCH r.owningDepartment d
    WHERE r.isActive = true
      AND r.resourceType = COALESCE(:resourceType, r.resourceType)
      AND f.id = COALESCE(:floorId, f.id)
      AND b.id = COALESCE(:buildingId, b.id)
      AND d.id = COALESCE(:departmentId, d.id)
      AND (
        r.resourceType <> com.example.vertexSpace.enums.ResourceType.ROOM
        OR r.capacity >= COALESCE(:capacityMin, r.capacity)
      )
    ORDER BY
      b.name,
      f.floorNumber,
      r.name
    """)
    List<Resource> findByFilters(
            @Param("resourceType") ResourceType resourceType,
            @Param("floorId") UUID floorId,
            @Param("buildingId") UUID buildingId,
            @Param("departmentId") UUID departmentId,
            @Param("capacityMin") Integer capacityMin
    );

    // ========================================================================
    // AVAILABILITY QUERIES (TIME-BASED CONFLICT DETECTION)
    // ========================================================================

    /**
     * Find all resources available in a time range (no filters)
     *
     * Returns resources that have NO conflicting bookings or offers
     * during the specified time window.
     *
     * Conflict detection includes buffer time (via conflictEndUtc).
     *
     * @param startTime Start of requested time slot
     * @param endTime End of requested time slot (should include buffer)
     * @return List of available active resources
     */
    @Query("""
        SELECT r FROM Resource r
        WHERE r.isActive = true
          AND r.id NOT IN (
              SELECT tb.resource.id
              FROM ResourceTimeBlock tb
              WHERE tb.status IN ('CONFIRMED', 'OFFERED')
                AND tb.startTimeUtc < :endTime
                AND tb.conflictEndUtc > :startTime
          )
        ORDER BY r.name
        """)
    List<Resource> findAvailableInTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Find available resources by multiple filters + time availability
     *
     * Combines filtering (type, location, capacity) with availability check.
     * This is the MAIN query used when users search for "available resources".
     *
     * Checks for conflicts with:
     * - CONFIRMED bookings
     * - OFFERED waitlist offers (not yet accepted)
     *
     * All filter parameters are optional. startTime and endTime are required.
     *
     * @param resourceType Filter by resource type (null = all types)
     * @param buildingId Filter by building (null = all buildings)
     * @param floorId Filter by floor (null = all floors)
     * @param departmentId Filter by department (null = all departments)
     * @param minCapacity Minimum capacity for rooms (null = no minimum)
     * @param startTime Start of requested time slot
     * @param endTime End of requested time slot (should include buffer)
     * @return List of available resources matching all criteria
     */
    @Query("""
    SELECT r FROM Resource r
    JOIN FETCH r.floor f
    JOIN FETCH f.building b
    JOIN FETCH r.owningDepartment d
    WHERE r.isActive = true
      AND r.resourceType = COALESCE(:resourceType, r.resourceType)
      AND b.id = COALESCE(:buildingId, b.id)
      AND f.id = COALESCE(:floorId, f.id)
      AND d.id = COALESCE(:departmentId, d.id)
      AND (
        r.resourceType <> com.example.vertexSpace.enums.ResourceType.ROOM
        OR r.capacity >= COALESCE(:minCapacity, r.capacity)
      )
      AND r.id NOT IN (
          SELECT tb.resource.id
          FROM ResourceTimeBlock tb
          WHERE tb.status IN ('CONFIRMED', 'OFFERED')
            AND tb.startTimeUtc < :endTime
            AND tb.conflictEndUtc > :startTime
      )
    ORDER BY
      b.name,
      f.floorNumber,
      r.name
    """)
    List<Resource> findAvailableByFilters(
            @Param("resourceType") ResourceType resourceType,
            @Param("buildingId") UUID buildingId,
            @Param("floorId") UUID floorId,
            @Param("departmentId") UUID departmentId,
            @Param("minCapacity") Integer minCapacity,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );


    // ========================================================================
    // AUTHORIZATION & STATISTICS
    // ========================================================================

    /**
     * Check if resource is owned by a specific department
     * Used for authorization checks (DEPT_ADMIN permissions)
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Resource r
        WHERE r.id = :resourceId
          AND r.owningDepartment.id = :departmentId
        """)
    boolean isOwnedByDepartment(
            @Param("resourceId") UUID resourceId,
            @Param("departmentId") UUID departmentId
    );

    /**
     * Count active resources by type
     * Returns list of [ResourceType, count] pairs
     * Used for dashboard statistics
     */
    @Query("""
        SELECT r.resourceType, COUNT(r)
        FROM Resource r
        WHERE r.isActive = true
        GROUP BY r.resourceType
        """)
    List<Object[]> countByResourceType();
    @Query("""
        SELECT DISTINCT r FROM Resource r
        LEFT JOIN FETCH r.floor f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH r.owningDepartment d
        WHERE r.isActive = true
          AND r.resourceType = :resourceType
          AND b.id = :buildingId
          AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
          AND NOT EXISTS (
            SELECT 1 FROM ResourceTimeBlock tb
            WHERE tb.resource.id = r.id
              AND tb.status IN (com.example.vertexSpace.enums.BlockStatus.CONFIRMED, 
                               com.example.vertexSpace.enums.BlockStatus.OFFERED)
              AND tb.startTimeUtc < :conflictEndTime
              AND tb.conflictEndUtc > :startTime
          )
        ORDER BY r.capacity ASC, r.name ASC
        """)
    List<Resource> findAvailableResourcesByFilters(
            @Param("resourceType") ResourceType resourceType,
            @Param("buildingId") UUID buildingId,
            @Param("minCapacity") Integer minCapacity,
            @Param("startTime") Instant startTime,
            @Param("conflictEndTime") Instant conflictEndTime
    );
}
