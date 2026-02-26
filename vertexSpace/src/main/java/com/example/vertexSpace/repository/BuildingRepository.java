package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findByIsActiveTrue();
    Optional<Building> findByNameIgnoreCase(String name);
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Building b " +
            "WHERE LOWER(b.name) = LOWER(:name) AND b.id != :excludeId")
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID excludeId);
    boolean existsByNameIgnoreCase(String name);
    List<Building> findByCityIgnoreCaseAndIsActiveTrue(String city);
    List<Building> findByStateIgnoreCaseAndIsActiveTrue(String state);
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Floor f WHERE f.building.id = :buildingId")
    boolean hasFloors(UUID buildingId);

    @Query("SELECT b, COUNT(f) as floorCount " +
            "FROM Building b " +
            "LEFT JOIN Floor f ON f.building.id = b.id " +
            "WHERE b.id = :buildingId " +
            "GROUP BY b")
    Optional<Object[]> findByIdWithFloorCount(UUID buildingId);

    Optional<Object> findByName(String businessHub);
}
