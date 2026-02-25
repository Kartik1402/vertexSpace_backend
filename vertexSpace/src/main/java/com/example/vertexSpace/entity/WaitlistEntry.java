package com.example.vertexSpace.entity;

import com.example.vertexSpace.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Waitlist Entry Entity
 *
 * Represents a user's request to be notified when a specific time slot becomes available.
 * Entries are processed in FIFO order (First In, First Out) based on created_at timestamp.
 *
 * Business Rules:
 * - One user can only join waitlist once per slot (UNIQUE constraint)
 * - Position in queue determined by created_at
 * - When slot becomes available, system creates OFFER_HOLD for next ACTIVE entry
 */
@Entity
@Table(
        name = "waitlist_entries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_waitlist_slot_user",
                        columnNames = {"resource_id", "start_utc", "end_utc", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_waitlist_fifo", columnList = "resource_id, start_utc, created_at"),
                @Index(name = "idx_waitlist_user", columnList = "user_id, status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Which resource they're waiting for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_booking_id")
    private ResourceTimeBlock pendingBooking;

    /**
     * Who's waiting
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Desired time slot (UTC)
     */
    @Column(name = "start_utc", nullable = false)
    private Instant startUtc;

    @Column(name = "end_utc")
    private Instant endUtc;

    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaitlistStatus status = WaitlistStatus.ACTIVE;

    /**
     * When they joined (determines queue position)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last status change
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * When offer was sent (nullable - only set when offer created)
     */
    @Column(name = "offered_at")
    private Instant offeredAt;

    /**
     * When offer was accepted/declined (nullable)
     */
    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;



    @Column(name = "requested_start_time")
    private Instant requestedStartTime;

    @Column(name = "requested_end_time")
    private Instant requestedEndTime;

    @Column(name = "purpose")
    private String purpose;


    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;
    /**
     * Check if the offer has expired
     *
     * An offer is considered expired if:
     * - Status is OFFER_SENT AND
     * - offerExpiresAt is in the past
     *
     * @return true if offer expired, false otherwise
     */
    public boolean isOfferExpired() {
        if (this.status != WaitlistStatus.OFFERED) {
            return false; // Not in offer state, can't be expired
        }

        if (this.offerExpiresAt == null) {
            return false; // No expiry set, not expired
        }

        return Instant.now().isAfter(this.offerExpiresAt);
    }
    /**
     * Get remaining minutes until offer expires
     *
     * Returns:
     * - Positive number: minutes remaining
     * - 0: expired or no offer
     * - null: not in offer state or no expiry set
     *
     * @return Minutes until expiry, or null if not applicable
     */
    public Long getMinutesUntilExpiry() {
        // Not in offer state
        if (this.status != WaitlistStatus.OFFERED) {
            return null;
        }

        // No expiry timestamp set
        if (this.offerExpiresAt == null) {
            return null;
        }

        Instant now = Instant.now();

        // Already expired
        if (now.isAfter(this.offerExpiresAt)) {
            return 0L;
        }

        // Calculate remaining minutes
        Duration remaining = Duration.between(now, this.offerExpiresAt);
        return remaining.toMinutes();
    }

}
