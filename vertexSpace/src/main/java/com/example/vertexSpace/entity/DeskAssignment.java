package com.example.vertexSpace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "desk_assignments", indexes = {
        @Index(name = "idx_desk_assignments_resource", columnList = "resource_id"),
        @Index(name = "idx_desk_assignments_user", columnList = "user_id"),
        @Index(name = "idx_desk_assignments_dates", columnList = "start_utc, end_utc")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_utc", nullable = false)
    private Instant startUtc;

    /**
     * End date (null = indefinite assignment)
     */
    @Column(name = "end_utc")
    private Instant endUtc;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods
    public boolean isIndefinite() {
        return endUtc == null;
    }

    public boolean isCurrentlyActive() {
        Instant now = Instant.now();
        return isActive &&
                now.isAfter(startUtc) &&
                (endUtc == null || now.isBefore(endUtc));
    }

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        // Check if this assignment overlaps with given time range
        Instant thisEnd = this.endUtc != null ? this.endUtc : Instant.MAX;
        Instant compareEnd = otherEnd != null ? otherEnd : Instant.MAX;

        return this.startUtc.isBefore(compareEnd) && thisEnd.isAfter(otherStart);
    }
}
