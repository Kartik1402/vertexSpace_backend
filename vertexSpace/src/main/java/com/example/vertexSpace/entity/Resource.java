package com.example.vertexSpace.entity;
import com.example.vertexSpace.enums.AssignmentMode;
import com.example.vertexSpace.enums.ResourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a bookable resource (room, desk, parking)
 * Ownership determines admin permissions, NOT booking restrictions
 */
@Builder
@Entity
@Table(
        name = "resources",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_resource_per_floor",
                columnNames = {"floor_id", "name"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owning_department_id", nullable = false)
    private Department owningDepartment;  // For admin control, not booking restriction

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode", nullable = false)
    @Builder.Default
    private AssignmentMode assignmentMode = AssignmentMode.NOT_APPLICABLE;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private Integer capacity;  // Required for ROOM, null for DESK/PARKING

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @UpdateTimestamp
    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    public boolean isDesk() {
        return resourceType == ResourceType.DESK;
    }

    public boolean isAssignableDesk() {
        return resourceType == ResourceType.DESK && assignmentMode == AssignmentMode.ASSIGNED;
    }

    public boolean isHotDesk() {
        return resourceType == ResourceType.DESK && assignmentMode == AssignmentMode.HOT_DESK;
    }

    public boolean isRoom() {
        return resourceType == ResourceType.ROOM;
    }

    public boolean isParking() {
        return resourceType == ResourceType.PARKING;
    }
}

