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
 * Entity representing a physical building/location
 * Managed by System Admins only
 */
@Entity
@Table(name = "buildings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(nullable = false, length = 100)
    private String country;

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
