package com.example.vertexSpace.entity;

import com.example.vertexSpace.enums.BlockStatus;
import com.example.vertexSpace.enums.BlockType;
import com.example.vertexSpace.enums.RecurrencePattern;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * UNIFIED table for bookings and waitlist offers
 * PostgreSQL exclusion constraint prevents overlapping active blocks
 *
 * UPDATED FOR MILESTONE 3:
 * - Added waitlistEntryId (links back to waitlist_entries)
 * - Added expiresAtUtc (for OFFER_HOLD expiry - 10 minutes)
 * - Added respondedAt (when user accepted/declined offer)
 * - Added notes (optional reason for cancellation/decline)
 */
@Entity
@Table(name = "resource_time_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Who booked or who has the offer

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlockType blockType;  // BOOKING or OFFER_HOLD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockStatus status;  // CONFIRMED, OFFERED, ACCEPTED, DECLINED, EXPIRED, CANCELLED

    @Column(name = "start_time_utc", nullable = false)
    private Instant startTimeUtc;

    @Column(name = "end_time_utc", nullable = false)
    private Instant endTimeUtc;

    /**
     * Computed field: end_time_utc + buffer_minutes
     * Used in exclusion constraint to prevent overlapping bookings
     */
    @Column(name = "conflict_end_utc", nullable = false)
    private Instant conflictEndUtc;

    @Column(name = "buffer_minutes", nullable = false)
    private Integer bufferMinutes = 15;  // Default 15-minute buffer

    @Column(columnDefinition = "TEXT")
    private String purpose;  // Optional: why booking was made

    // ========================================================================
    // MILESTONE 3: NEW FIELDS
    // ========================================================================

    /**
     * Link back to waitlist entry (if this came from waitlist)
     * Nullable: regular bookings won't have this
     */
    @Column(name = "waitlist_entry_id")
    private UUID waitlistEntryId;

    /**
     * When this offer expires (only for OFFER_HOLD type)
     * Typically created_at_utc + 10 minutes
     * Nullable: regular bookings don't expire
     */
    @Column(name = "expires_at_utc")
    private Instant expiresAtUtc;

    /**
     * When user responded to offer (accepted/declined)
     * Nullable: only set when user takes action
     */
    @Column(name = "responded_at")
    private Instant respondedAt;

    /**
     * Optional: Reason for cancellation/decline
     */
    @Column(name = "notes", length = 500)
    private String notes;

    // ========================================================================
    // AUDIT FIELDS
    // ========================================================================

    @CreationTimestamp
    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @UpdateTimestamp
    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    /**
     * Helper method to calculate conflict_end_utc before saving
     */
    // ========================================================================
// RECURRING BOOKING FIELDS
// ========================================================================

    @Column(name = "recurring_series_id")
    private UUID recurringSeriesId;  // Groups related bookings

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;  // Default: single booking

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_pattern", length = 20)
    private RecurrencePattern recurrencePattern;  // WEEKLY, DAILY, MONTHLY, NONE

    @Column(name = "occurrence_number")
    private Integer occurrenceNumber;  // 1, 2, 3... for each instance

    @Column(name = "original_timezone", length = 50)
    private String originalTimezone = "Asia/Kolkata";  // User's timezone

    @PrePersist
    @PreUpdate
    public void calculateConflictEnd() {
        if (endTimeUtc != null && bufferMinutes != null) {
            this.conflictEndUtc = endTimeUtc.plusSeconds(bufferMinutes * 60L);
        }
    }
}
