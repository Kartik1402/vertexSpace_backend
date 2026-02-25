package com.example.vertexSpace.repository;
import com.example.vertexSpace.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByCode(String code);
    boolean existsByCode(String code);
    Optional<Department> findByName(String name);

    // ADD THIS NEW METHOD - Required for DatabaseSeeder
}
