package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Resource;
import com.example.vertexSpace.entity.ResourceTimeBlock;
import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ResourceTimeBlock entity
 *
 * Includes methods for:
 * - Milestone 2: Booking CRUD and conflict detection
 * - Milestone 3: Waitlist offers and expiry management
 */
@Repository
public interface ResourceTimeBlockRepository extends JpaRepository<ResourceTimeBlock, UUID> {

    // ========================================================================
    // MILESTONE 2: BOOKING METHODS
    // ========================================================================

    /**
     * Lock resource for booking (pessimistic lock to prevent race conditions)
     * Returns the Resource entity, not ResourceTimeBlock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :resourceId")
    Optional<Resource> lockResourceForBooking(@Param("resourceId") UUID resourceId);

    /**
     * Check if resource has overlapping blocks in time range
     */
    @Query("""
        SELECT COUNT(b) > 0
        FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status IN ('CONFIRMED', 'OFFERED')
          AND b.startTimeUtc < :endTime
          AND b.conflictEndUtc > :startTime
        """)
    boolean existsOverlappingBlock(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Find overlapping blocks (for conflict messages)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status IN ('CONFIRMED', 'OFFERED')
          AND b.startTimeUtc < :endTime
          AND b.conflictEndUtc > :startTime
        ORDER BY b.startTimeUtc ASC
        """)
    List<ResourceTimeBlock> findOverlappingBlocks(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Check if user has overlapping booking (prevent double-booking)
     */
    @Query("""
        SELECT COUNT(b) > 0
        FROM ResourceTimeBlock b
        WHERE b.user.id = :userId
          AND b.blockType = 'BOOKING'
          AND b.status = 'CONFIRMED'
          AND b.startTimeUtc < :endTime
          AND b.conflictEndUtc > :startTime
        """)
    boolean userHasOverlappingBooking(
            @Param("userId") UUID userId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    @Query("""
    SELECT COUNT(b)
    FROM ResourceTimeBlock b
    WHERE b.resource.id = :resourceId
      AND b.blockType = 'BOOKING'
      AND b.status = 'CONFIRMED'
      AND b.endTimeUtc > CURRENT_TIMESTAMP
    """)
    long countActiveBookings(@Param("resourceId") UUID resourceId);
    /**
     * Find booking by ID with relations (eager fetch to avoid N+1)
     */
    /**
     * Find conflicting time blocks for a resource
     */
    @Query("""
    SELECT tb FROM ResourceTimeBlock tb
    WHERE tb.resource.id = :resourceId
      AND tb.status IN ('CONFIRMED', 'OFFERED')
      AND tb.startTimeUtc < :endTime
      AND tb.conflictEndUtc > :startTime
    ORDER BY tb.startTimeUtc
    """)
    List<ResourceTimeBlock> findConflictingBlocks(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        JOIN FETCH b.user
        WHERE b.id = :bookingId
        """)
    Optional<ResourceTimeBlock> findByIdWithRelations(@Param("bookingId") UUID bookingId);

    /**
     * Find all user bookings (all statuses, all times)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        WHERE b.user.id = :userId
          AND b.blockType = 'BOOKING'
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findUserBookings(@Param("userId") UUID userId);

    /**
     * Find user's upcoming bookings (after current time)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        WHERE b.user.id = :userId
          AND b.blockType = 'BOOKING'
          AND b.status = 'CONFIRMED'
          AND b.startTimeUtc > :now
        ORDER BY b.startTimeUtc ASC
        """)
    List<ResourceTimeBlock> findUserUpcomingBookings(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Find user's past bookings (before current time)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        WHERE b.user.id = :userId
          AND b.blockType = 'BOOKING'
          AND b.endTimeUtc < :now
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findUserPastBookings(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Find all bookings for a resource
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.user
        WHERE b.resource.id = :resourceId
          AND b.blockType = 'BOOKING'
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findResourceBookings(@Param("resourceId") UUID resourceId);

    /**
     * Find resource bookings in time range
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.user
        WHERE b.resource.id = :resourceId
          AND b.blockType = 'BOOKING'
          AND b.status = 'CONFIRMED'
          AND b.startTimeUtc < :endTime
          AND b.endTimeUtc > :startTime
        ORDER BY b.startTimeUtc ASC
        """)
    List<ResourceTimeBlock> findResourceBookingsInRange(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Cancel booking (update to CANCELLED status)
     */
    @Modifying
    @Query("""
        UPDATE ResourceTimeBlock b
        SET b.status = 'CANCELLED',
            b.updatedAtUtc = :cancelledAt
        WHERE b.id = :bookingId
          AND b.status = 'CONFIRMED'
        """)
    int cancelBooking(
            @Param("bookingId") UUID bookingId,
            @Param("cancelledAt") Instant cancelledAt
    );

    /**
     * Find all bookings (admin view)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        JOIN FETCH b.user u
        JOIN FETCH u.department
        WHERE b.blockType = 'BOOKING'
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findAllBookings();

    /**
     * Find bookings for a department's resources
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        JOIN FETCH b.user
        WHERE r.owningDepartment.id = :departmentId
          AND b.blockType = 'BOOKING'
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findDepartmentBookings(@Param("departmentId") UUID departmentId);

    // ========================================================================
    // MILESTONE 2: HELPER METHOD (used internally by conflict detection)
    // ========================================================================

    /**
     * Check if resource has conflicting blocks (used by BookingValidator)
     * Alias for existsOverlappingBlock
     */
    @Query("""
        SELECT COUNT(b) > 0
        FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status IN ('CONFIRMED', 'OFFERED')
          AND b.startTimeUtc < :endTime
          AND b.conflictEndUtc > :startTime
        """)
    boolean hasConflict(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Find blocks by resource, status, and time range
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status = :status
          AND b.startTimeUtc >= :startTime
          AND b.startTimeUtc < :endTime
        ORDER BY b.startTimeUtc ASC
        """)
    List<ResourceTimeBlock> findByResourceIdAndStatusAndStartTimeBetween(
            @Param("resourceId") UUID resourceId,
            @Param("status") BlockStatus status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Find user's bookings by status
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        WHERE b.user.id = :userId
          AND b.status = :status
        ORDER BY b.startTimeUtc DESC
        """)
    List<ResourceTimeBlock> findByUserIdAndStatusOrderByStartTimeDesc(
            @Param("userId") UUID userId,
            @Param("status") BlockStatus status
    );

    /**
     * Find all blocks for resource in time range (including OFFERED)
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status IN ('CONFIRMED', 'OFFERED')
          AND b.startTimeUtc < :endTime
          AND b.endTimeUtc > :startTime
        ORDER BY b.startTimeUtc ASC
        """)
    List<ResourceTimeBlock> findBlocksInTimeRange(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    // ========================================================================
    // MILESTONE 3: WAITLIST & OFFER METHODS
    // ========================================================================

    /**
     * Find with pessimistic lock (for offer acceptance)
     *
     * Locks row to prevent race conditions during offer processing.
     * Used in acceptOffer() transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ResourceTimeBlock b WHERE b.id = :id")
    Optional<ResourceTimeBlock> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Check conflict excluding specific block (for offer acceptance)
     *
     * Used to verify slot is still available when accepting offer.
     * Excludes the offer itself from conflict check.
     */
    @Query("""
        SELECT COUNT(b) > 0
        FROM ResourceTimeBlock b
        WHERE b.resource.id = :resourceId
          AND b.status IN ('CONFIRMED', 'OFFERED')
          AND b.startTimeUtc < :endTime
          AND b.conflictEndUtc > :startTime
          AND b.id != :excludeId
        """)
    boolean existsActiveBlockExcluding(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeId") UUID excludeId
    );

    /**
     * Find user's active offers (for GET /api/v1/me/waitlist-offers)
     *
     * Returns offers that:
     * - Belong to user
     * - Are OFFER_HOLD type
     * - Status is OFFERED
     * - Haven't expired yet
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH r.floor f
        JOIN FETCH f.building
        WHERE b.user.id = :userId
          AND b.blockType = 'OFFER_HOLD'
          AND b.status = 'OFFERED'
          AND b.expiresAtUtc > :now
        ORDER BY b.expiresAtUtc ASC
        """)
    List<ResourceTimeBlock> findActiveOffers(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Find expired offers (for scheduler)
     *
     * Returns OFFERED blocks past their expiry time.
     * Scheduler processes these every minute.
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        JOIN FETCH b.resource r
        JOIN FETCH b.user u
        WHERE b.blockType = 'OFFER_HOLD'
          AND b.status = 'OFFERED'
          AND b.expiresAtUtc <= :now
        """)
    List<ResourceTimeBlock> findExpiredOffers(@Param("now") Instant now);

    /**
     * Find offer by waitlist entry ID
     */
    @Query("""
        SELECT b FROM ResourceTimeBlock b
        WHERE b.waitlistEntryId = :waitlistEntryId
          AND b.blockType = :blockType
        """)
    Optional<ResourceTimeBlock> findByWaitlistEntryIdAndBlockType(
            @Param("waitlistEntryId") UUID waitlistEntryId,
            @Param("blockType") BlockType blockType
    );

    /**
     * Count user's confirmed bookings in date range (for recommendations)
     *
     * Returns aggregated data: resource info + booking count + last booked time
     */
    @Query("""
        SELECT b.resource.id AS resourceId,
               b.resource.name AS resourceName,
               CAST(b.resource.resourceType AS string) AS resourceType,
               COUNT(b) AS bookingCount,
               MAX(b.startTimeUtc) AS lastBookedAt
        FROM ResourceTimeBlock b
        WHERE b.user.id = :userId
          AND b.blockType = 'BOOKING'
          AND b.status = 'CONFIRMED'
          AND b.startTimeUtc >= :since
        GROUP BY b.resource.id, b.resource.name, b.resource.resourceType
        ORDER BY bookingCount DESC
        """)
    List<Object[]> findTopBookedResources(
            @Param("userId") UUID userId,
            @Param("since") Instant since
    );

    /**
     * Cancel all active offers for specific waitlist entry
     * Used when user leaves waitlist
     */
    @Modifying
    @Query("""
        UPDATE ResourceTimeBlock b
        SET b.status = 'CANCELLED',
            b.respondedAt = :respondedAt,
            b.updatedAtUtc = :respondedAt
        WHERE b.waitlistEntryId = :waitlistEntryId
          AND b.blockType = 'OFFER_HOLD'
          AND b.status = 'OFFERED'
        """)
    int cancelOffersByWaitlistEntry(
            @Param("waitlistEntryId") UUID waitlistEntryId,
            @Param("respondedAt") Instant respondedAt
    );
    /**
     * Find all bookings in a recurring series, ordered by occurrence number
     */
    @Query("""
    SELECT b FROM ResourceTimeBlock b
    JOIN FETCH b.resource r
    JOIN FETCH r.floor f
    JOIN FETCH f.building
    JOIN FETCH b.user
    WHERE b.recurringSeriesId = :seriesId
    ORDER BY b.occurrenceNumber ASC
    """)
    List<ResourceTimeBlock> findByRecurringSeriesIdOrderByOccurrenceNumber(@Param("seriesId") UUID seriesId);

    /**
     * Find future bookings in a recurring series
     */
    @Query("""
    SELECT b FROM ResourceTimeBlock b
    WHERE b.recurringSeriesId = :seriesId
      AND b.startTimeUtc > :afterTime
      AND b.status = 'CONFIRMED'
    ORDER BY b.occurrenceNumber ASC
    """)
    List<ResourceTimeBlock> findByRecurringSeriesIdAndStartTimeAfter(
            @Param("seriesId") UUID seriesId,
            @Param("afterTime") Instant afterTime
    );

    /**
     * Count bookings in a series
     */
    @Query("SELECT COUNT(b) FROM ResourceTimeBlock b WHERE b.recurringSeriesId = :seriesId")
    long countByRecurringSeriesId(@Param("seriesId") UUID seriesId);

}
