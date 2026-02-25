package com.example.vertexSpace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a floor within a building
 * Managed by System Admins only
 */
@Entity
@Table(
        name = "floors",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_floor_per_building",
                columnNames = {"building_id", "floor_number"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;  // Can be negative for basements (e.g., -1, -2)

    @Column(name = "floor_name", nullable = false, length = 100)
    private String floorName;  // e.g., "Ground Floor", "1st Floor", "Basement Level 1"

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at_utc", nullable = false, updatable = false)
    private Instant createdAtUtc;

    @UpdateTimestamp
    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;
}
