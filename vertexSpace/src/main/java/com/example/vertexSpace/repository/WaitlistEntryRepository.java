package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.WaitlistEntry;
import com.example.vertexSpace.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Waitlist Entry Repository
 *
 * Key query: findNextInQueue - FIFO ordering for offer generation
 */
@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    /**
     * Find next person in queue (FIFO: First In, First Out)
     *
     * Returns earliest ACTIVE entry for specific slot.
     * Used when generating offers after cancellation.
     *
     * @param resourceId Which resource
     * @param startUtc Slot start time
     * @param endUtc Slot end time
     * @param status Only ACTIVE entries
     * @return Next person in line
     */
    @Query("""
        SELECT w FROM WaitlistEntry w
        WHERE w.resource.id = :resourceId
          AND w.startUtc = :startUtc
          AND w.endUtc = :endUtc
          AND w.status = :status
        ORDER BY w.createdAt ASC
        LIMIT 1
        """)
    Optional<WaitlistEntry> findNextInQueue(
            @Param("resourceId") UUID resourceId,
            @Param("startUtc") Instant startUtc,
            @Param("endUtc") Instant endUtc,
            @Param("status") WaitlistStatus status
    );

    /**
     * Get user's position in queue
     *
     * Counts how many people joined before this user.
     *
     * @param resourceId Which resource
     * @param startUtc Slot start
     * @param endUtc Slot end
     * @param createdAt User's join time
     * @return Number of people ahead + 1 = position
     */
    @Query("""
        SELECT COUNT(w) + 1
        FROM WaitlistEntry w
        WHERE w.resource.id = :resourceId
          AND w.startUtc = :startUtc
          AND w.endUtc = :endUtc
          AND w.status = 'ACTIVE'
          AND w.createdAt < :createdAt
        """)
    Long getPositionInQueue(
            @Param("resourceId") UUID resourceId,
            @Param("startUtc") Instant startUtc,
            @Param("endUtc") Instant endUtc,
            @Param("createdAt") Instant createdAt
    );

    /**
     * Check if user already in waitlist for specific slot
     *
     * Prevents duplicate entries (also enforced by UNIQUE constraint).
     */
    boolean existsByResourceIdAndStartUtcAndEndUtcAndUserIdAndStatus(
            UUID resourceId,
            Instant startUtc,
            Instant endUtc,
            UUID userId,
            WaitlistStatus status
    );

    /**
     * Find user's waitlist entries with filters
     */
    @Query("""
    SELECT we
    FROM WaitlistEntry we
    WHERE we.user.id = COALESCE(:userId, we.user.id)
      AND we.resource.id = COALESCE(:resourceId, we.resource.id)
      AND we.status = COALESCE(:status, we.status)
      AND we.startUtc >= COALESCE(:startUtc, we.startUtc)
    ORDER BY we.createdAt DESC
    """)
    List<WaitlistEntry> findWithFilters(
            @Param("userId") UUID userId,
            @Param("resourceId") UUID resourceId,
            @Param("status") WaitlistStatus status,
            @Param("startUtc") Instant startUtc
    );
    /**
     * Find active entries for past time slots (need to expire)
     */
    @Query("""
    SELECT we FROM WaitlistEntry we
    WHERE we.status = 'ACTIVE'
      AND we.endUtc < :now
    """)
    List<WaitlistEntry> findActiveEntriesForPastTimeSlots(@Param("now") Instant now);

    /**
     * Find expired offers (status = OFFERED but offerExpiresAt < now)
     */
//    @Query("""
//    SELECT we FROM WaitlistEntry we
//    WHERE we.status = 'OFFERED'
//      AND we.offerExpiresAt < :now
//      OR (we.offeredAt+durations(10))<:now
//    """)
//    List<WaitlistEntry> findExpiredOffers(@Param("now") Instant now);
    @Query(value = """
  SELECT *
  FROM waitlist_entries we
  WHERE we.status = 'OFFERED'
    AND (
      (we.offer_expires_at IS NOT NULL AND we.offer_expires_at < :now)
      OR
      (we.offered_at IS NOT NULL AND we.offered_at + interval '10 minutes' < :now)
    )
  """, nativeQuery = true)
    List<WaitlistEntry> findExpiredOffers(@Param("now") Instant now);

    /**
     * Find entries by resource and status, ordered by creation time
     */
    List<WaitlistEntry> findByResourceIdAndStatusOrderByCreatedAt(
            UUID resourceId,
            WaitlistStatus status
    );

    /**
     * Count active entries for a resource (for queue position calculation)
     */
    @Query("""
    SELECT COUNT(we) FROM WaitlistEntry we
    WHERE we.resource.id = :resourceId
      AND we.status = 'ACTIVE'
    """)
    Integer countActiveEntriesForResource(@Param("resourceId") UUID resourceId);




    /**
     * Update entry status (for offer fulfillment)
     */
    @Modifying
    @Query("""
        UPDATE WaitlistEntry w
        SET w.status = :status,
            w.fulfilledAt = :fulfilledAt
        WHERE w.id = :entryId
        """)
    void updateStatus(
            @Param("entryId") UUID entryId,
            @Param("status") WaitlistStatus status,
            @Param("fulfilledAt") Instant fulfilledAt
    );
    @Query("""
    SELECT w FROM WaitlistEntry w
    WHERE w.resource.id = :resourceId
    AND w.status = 'ACTIVE'
    AND w.startUtc < :endTime
    AND w.endUtc > :startTime
    ORDER BY w.queuePosition ASC
    """)
    List<WaitlistEntry> findActiveEntriesForResourceAndTimeRange(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
    /**
     * Expire old entries (slots that have passed)
     *
     * Called by scheduler to clean up entries for past time slots.
     */
    @Modifying
    @Query("""
        UPDATE WaitlistEntry w
        SET w.status = 'EXPIRED'
        WHERE w.status = 'ACTIVE'
          AND w.startUtc < :now
        """)
    int expireOldEntries(@Param("now") Instant now);

    /**
     * Get all active entries for a user
     */
    List<WaitlistEntry> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId,
            WaitlistStatus status
    );

}
