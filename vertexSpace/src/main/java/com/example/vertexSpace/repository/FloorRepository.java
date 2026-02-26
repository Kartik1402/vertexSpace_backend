package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByIsActiveTrue();
    @Query("SELECT f FROM Floor f " +
            "WHERE f.building.id = :buildingId " +
            "ORDER BY f.floorNumber")
    List<Floor> findByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT f FROM Floor f " +
            "WHERE f.building.id = :buildingId AND f.isActive = TRUE " +
            "ORDER BY f.floorNumber")
    List<Floor> findByBuildingIdAndIsActiveTrue(@Param("buildingId") UUID buildingId);
    @Query("SELECT f FROM Floor f " +
            "WHERE f.building.id = :buildingId AND f.floorNumber = :floorNumber")
    Optional<Floor> findByBuildingIdAndFloorNumber(
            @Param("buildingId") UUID buildingId,
            @Param("floorNumber") Integer floorNumber
    );

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Floor f " +
            "WHERE f.building.id = :buildingId " +
            "AND f.floorNumber = :floorNumber " +
            "AND f.id != :excludeId")
    boolean existsByBuildingIdAndFloorNumberAndIdNot(
            @Param("buildingId") UUID buildingId,
            @Param("floorNumber") Integer floorNumber,
            @Param("excludeId") UUID excludeId
    );

    boolean existsByBuildingIdAndFloorNumber(UUID buildingId, Integer floorNumber);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Resource r WHERE r.floor.id = :floorId")
    boolean hasResources(UUID floorId);


    @Query("SELECT f, COUNT(r) as resourceCount " +
            "FROM Floor f " +
            "LEFT JOIN Resource r ON r.floor.id = f.id " +
            "WHERE f.id = :floorId " +
            "GROUP BY f")
    Optional<Object[]> findByIdWithResourceCount(UUID floorId);
    @Query("SELECT f FROM Floor f " +
            "JOIN FETCH f.building " +
            "WHERE f.id = :floorId")
    Optional<Floor> findByIdWithBuilding(@Param("floorId") UUID floorId);
    @Query("SELECT COUNT(f) FROM Floor f WHERE f.building.id = :buildingId")
    long countByBuildingId(@Param("buildingId") UUID buildingId);
}
