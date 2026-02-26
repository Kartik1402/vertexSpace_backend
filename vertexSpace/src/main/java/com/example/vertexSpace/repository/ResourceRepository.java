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
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findByIsActiveTrue();
    @Query("""
        SELECT r FROM Resource r
        WHERE r.floor.id = :floorId
        ORDER BY r.name
        """)
    List<Resource> findByFloorId(@Param("floorId") UUID floorId);
    @Query("""
        SELECT r FROM Resource r
        WHERE r.floor.id = :floorId
          AND r.isActive = true
        ORDER BY r.name
        """)
    List<Resource> findByFloorIdAndIsActiveTrue(@Param("floorId") UUID floorId);
    @Query("""
        SELECT r FROM Resource r
        JOIN r.floor f
        WHERE f.building.id = :buildingId
        ORDER BY f.floorNumber, r.name
        """)
    List<Resource> findByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("""
        SELECT r FROM Resource r
        JOIN r.floor f
        WHERE f.building.id = :buildingId
          AND r.isActive = true
        ORDER BY f.floorNumber, r.name
        """)
    List<Resource> findByBuildingIdAndIsActiveTrue(@Param("buildingId") UUID buildingId);

    List<Resource> findByResourceTypeAndIsActiveTrue(ResourceType resourceType);
    @Query("""
        SELECT r FROM Resource r
        WHERE r.owningDepartment.id = :departmentId
          AND r.isActive = true
        ORDER BY r.name
        """)
    List<Resource> findByOwningDepartmentIdAndIsActiveTrue(@Param("departmentId") UUID departmentId);
    @Query("""
        SELECT r FROM Resource r
        WHERE r.resourceType = com.example.vertexSpace.enums.ResourceType.ROOM
          AND r.capacity >= :minCapacity
          AND r.isActive = true
        ORDER BY r.capacity, r.name
        """)
    List<Resource> findRoomsByMinCapacity(@Param("minCapacity") Integer minCapacity);

    @Query("""
        SELECT r FROM Resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        JOIN FETCH r.owningDepartment
        WHERE r.id = :resourceId
        """)
    Optional<Resource> findByIdWithRelations(@Param("resourceId") UUID resourceId);

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
      AND r.assignmentMode!='ASSIGNED'
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
