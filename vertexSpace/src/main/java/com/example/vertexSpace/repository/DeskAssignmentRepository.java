package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.DeskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeskAssignmentRepository extends JpaRepository<DeskAssignment, UUID> {

    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.resource.id = :deskId
        AND da.isActive = true
        AND da.startUtc <= :checkTime
        AND (da.endUtc IS NULL OR da.endUtc > :checkTime)
        """)
    Optional<DeskAssignment> findActiveAssignmentForDesk(
            @Param("deskId") UUID deskId,
            @Param("checkTime") Instant checkTime
    );


    @Query("""
        SELECT COUNT(da) FROM DeskAssignment da
        WHERE da.resource.id = :deskId
        AND da.isActive = true
        AND da.id != :excludeId
        AND da.startUtc < :endTime
        AND (da.endUtc IS NULL OR da.endUtc > :startTime)
        """)
    long countOverlappingAssignments(
            @Param("deskId") UUID deskId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeId") UUID excludeId
    );
    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.resource.id = :deskId
        ORDER BY da.startUtc DESC
        """)
    Page<DeskAssignment> findByDeskId(
            @Param("deskId") UUID deskId,
            Pageable pageable
    );

    /**
     * Find all assignments for a user
     */
    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.user.id = :userId
        AND da.isActive = true
        ORDER BY da.startUtc DESC
        """)
    List<DeskAssignment> findActiveAssignmentsByUserId(@Param("userId") UUID userId);

    /**
     * Find current active assignment for a user
     */
    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.user.id = :userId
        AND da.isActive = true
        AND da.startUtc <= :now
        AND (da.endUtc IS NULL OR da.endUtc > :now)
        """)
    Optional<DeskAssignment> findCurrentAssignmentForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Find all assignments in a department
     */
    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.resource.owningDepartment = :departmentId
        AND da.isActive = true
        ORDER BY da.resource.name, da.startUtc DESC
        """)
    Page<DeskAssignment> findByDepartmentId(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );
    @Query("""
        SELECT COUNT(da) FROM DeskAssignment da
        WHERE da.resource.id = :deskId
        AND da.isActive = true
        AND da.startUtc <= :now
        AND (da.endUtc IS NULL OR da.endUtc > :now)
        """)
    long countActiveAssignmentsForDesk(
            @Param("deskId") UUID deskId,
            @Param("now") Instant now
    );

    @Query("""
        SELECT da FROM DeskAssignment da
        WHERE da.isActive = true
        AND da.endUtc IS NOT NULL
        AND da.endUtc BETWEEN :startTime AND :endTime
        ORDER BY da.endUtc ASC
        """)
    List<DeskAssignment> findAssignmentsExpiringSoon(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    @Modifying
    @Query("""
        UPDATE DeskAssignment da
        SET da.isActive = false
        WHERE da.id = :assignmentId
        """)
    int deactivateAssignment(@Param("assignmentId") UUID assignmentId);
}
