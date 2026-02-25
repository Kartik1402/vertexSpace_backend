package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Building entity operations
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    /**
     * Find all active buildings
     */
    List<Building> findByIsActiveTrue();

    /**
     * Find building by name (case-insensitive)
     */
    Optional<Building> findByNameIgnoreCase(String name);

    /**
     * Check if building name exists (case-insensitive, excluding specific ID)
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Building b " +
            "WHERE LOWER(b.name) = LOWER(:name) AND b.id != :excludeId")
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID excludeId);

    /**
     * Check if building name exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find buildings by city
     */
    List<Building> findByCityIgnoreCaseAndIsActiveTrue(String city);

    /**
     * Find buildings by state
     */
    List<Building> findByStateIgnoreCaseAndIsActiveTrue(String state);

    /**
     * Check if building has any floors
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Floor f WHERE f.building.id = :buildingId")
    boolean hasFloors(UUID buildingId);

    /**
     * Get building with floor count
     */
    @Query("SELECT b, COUNT(f) as floorCount " +
            "FROM Building b " +
            "LEFT JOIN Floor f ON f.building.id = b.id " +
            "WHERE b.id = :buildingId " +
            "GROUP BY b")
    Optional<Object[]> findByIdWithFloorCount(UUID buildingId);

    Optional<Object> findByName(String businessHub);
}
