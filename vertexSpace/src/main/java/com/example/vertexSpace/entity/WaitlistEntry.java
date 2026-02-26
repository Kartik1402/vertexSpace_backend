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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_booking_id")
    private ResourceTimeBlock pendingBooking;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "start_utc", nullable = false)
    private Instant startUtc;

    @Column(name = "end_utc")
    private Instant endUtc;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaitlistStatus status = WaitlistStatus.ACTIVE;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "offered_at")
    private Instant offeredAt;
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

    public boolean isOfferExpired() {
        if (this.status != WaitlistStatus.OFFERED) {
            return false; // Not in offer state, can't be expired
        }

        if (this.offerExpiresAt == null) {
            return false; // No expiry set, not expired
        }

        return Instant.now().isAfter(this.offerExpiresAt);
    }
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
