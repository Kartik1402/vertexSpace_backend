//package com.example.vertexSpace.config;
//
//import com.example.vertexSpace.entity.Building;
//import com.example.vertexSpace.entity.Department;
//import com.example.vertexSpace.entity.Floor;
//import com.example.vertexSpace.entity.Resource;
//import com.example.vertexSpace.enums.ResourceType;
//import com.example.vertexSpace.repository.BuildingRepository;
//import com.example.vertexSpace.repository.DepartmentRepository;
//import com.example.vertexSpace.repository.FloorRepository;
//import com.example.vertexSpace.repository.ResourceRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
///**
// * Database Seeder for VertexSpace Booking System - Option 3
// *
// * This version ONLY seeds buildings, floors, and resources
// * It uses your EXISTING departments and users
// *
// * IMPORTANT: Update the DEPARTMENT_CODE constant below to match your existing department
// *
// * To enable: Set spring.profiles.active=dev in application.properties
// */
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//@Profile({"dev", "local"}) // Only run in dev/local environments
//public class DatabaseSeeder {
//
//    // ========================================================================
//    // CONFIGURATION - UPDATE THIS WITH YOUR DEPARTMENT CODE
//    // ========================================================================
//    private static final String DEPARTMENT_CODE = "IT";  // <-- CHANGE THIS to your department code
//
//    private final DepartmentRepository departmentRepository;
//    private final BuildingRepository buildingRepository;
//    private final FloorRepository floorRepository;
//    private final ResourceRepository resourceRepository;
//
//    @Bean
//    public CommandLineRunner seedDatabase() {
//        return args -> {
//            log.info("🌱 Starting database seeding (Option 3: Using existing departments/users)...");
//
//            // ========================================================================
//            // CHECK: Skip if buildings already exist
//            // ========================================================================
//            if (buildingRepository.count() > 0) {
//                log.info("✅ Buildings already exist. Skipping seeding...");
//                log.info("📊 Current counts:");
//                log.info("   - Departments: {}", departmentRepository.count());
//                log.info("   - Buildings: {}", buildingRepository.count());
//                log.info("   - Floors: {}", floorRepository.count());
//                log.info("   - Resources: {}", resourceRepository.count());
//                return;
//            }
//
//            try {
//                // ========================================================================
//                // Step 1: Get existing department
//                // ========================================================================
//                log.info("🏢 Looking for existing department with code: '{}'...", DEPARTMENT_CODE);
//
//                Department existingDept = departmentRepository.findByCode(DEPARTMENT_CODE)
//                        .orElseThrow(() -> new RuntimeException(
//                                String.format("Department with code '%s' not found! Please:\n" +
//                                                "1. Create a department with code '%s' in your database, OR\n" +
//                                                "2. Update DEPARTMENT_CODE constant in DatabaseSeeder.java to match your existing department code",
//                                        DEPARTMENT_CODE, DEPARTMENT_CODE)
//                        ));
//
//                log.info("✅ Found existing department: {} (ID: {})",
//                        existingDept.getName(), existingDept.getId());
//
//                // ========================================================================
//                // Step 2: Seed Buildings
//                // ========================================================================
//                Building building1 = seedBuildings();
//                Building building2 = getSecondBuilding(); // Get reference to second building
//
//                // ========================================================================
//                // Step 3: Seed Floors
//                // ========================================================================
//                Floor floor1 = seedFloors(building1);
//                Floor floor2 = getSecondFloor(building1);
//                Floor floor3 = getThirdFloor(building1);
//
//                // ========================================================================
//                // Step 4: Seed Resources
//                // ========================================================================
//                seedResources(floor1, existingDept);
//
//                // ========================================================================
//                // Success summary
//                // ========================================================================
//                log.info("✅ Database seeding completed successfully!");
//                log.info("📊 Summary:");
//                log.info("   - Departments: {} (EXISTING - not modified)", departmentRepository.count());
//                log.info("   - Buildings: {} (NEWLY CREATED)", buildingRepository.count());
//                log.info("   - Floors: {} (NEWLY CREATED)", floorRepository.count());
//                log.info("   - Resources: {} (NEWLY CREATED)", resourceRepository.count());
//                log.info("");
//                log.info("🎯 Seeded data is ready to use!");
//                log.info("   Building: {}", building1.getName());
//                log.info("   Floors: {}", floorRepository.count());
//                log.info("   Resources: {} (3 rooms, 5 desks, 3 parking)", resourceRepository.count());
//
//            } catch (Exception e) {
//                log.error("❌ Error during database seeding: {}", e.getMessage(), e);
//                log.error("💡 Troubleshooting tips:");
//                log.error("   1. Check if department with code '{}' exists in your database", DEPARTMENT_CODE);
//                log.error("   2. Verify database connection is working");
//                log.error("   3. Check application logs for detailed error messages");
//                throw e;
//            }
//        };
//    }
//
//    // ========================================================================
//    // SEED BUILDINGS
//    // ========================================================================
//    private Building seedBuildings() {
//        log.info("🏗️ Seeding buildings...");
//
//        Building building1 = new Building();
//        building1.setName("Tech Tower");
//        building1.setAddress("123 Innovation Drive");
//        building1.setCity("San Francisco");
//        building1.setState("CA");
//        building1.setZipCode("94105");
//        building1.setCountry("USA");
//        building1.setIsActive(true);
//        building1 = buildingRepository.save(building1);
//        log.info("   ✓ Created: {} (ID: {})", building1.getName(), building1.getId());
//
//        Building building2 = new Building();
//        building2.setName("Business Hub");
//        building2.setAddress("456 Commerce Street");
//        building2.setCity("New York");
//        building2.setState("NY");
//        building2.setZipCode("10001");
//        building2.setCountry("USA");
//        building2.setIsActive(true);
//        building2 = buildingRepository.save(building2);
//        log.info("   ✓ Created: {} (ID: {})", building2.getName(), building2.getId());
//
//        log.info("✅ Buildings seeded: 2 buildings created");
//        return building1;
//    }
//
//    private Building getSecondBuilding() {
//        return (Building) buildingRepository.findByName("Business Hub")
//                .orElseThrow(() -> new RuntimeException("Business Hub building not found"));
//    }
//
//    // ========================================================================
//    // SEED FLOORS
//    // ========================================================================
//    private Floor seedFloors(Building building1) {
//        log.info("🏢 Seeding floors for building: {}...", building1.getName());
//
//        Floor floor1 = new Floor();
//        floor1.setBuilding(building1);
//        floor1.setFloorNumber(1);
//        floor1.setFloorName("Ground Floor");
//        floor1.setIsActive(true);
//        floor1 = floorRepository.save(floor1);
//        log.info("   ✓ Created: {} - Floor {} (ID: {})",
//                building1.getName(), floor1.getFloorNumber(), floor1.getId());
//
//        Floor floor2 = new Floor();
//        floor2.setBuilding(building1);
//        floor2.setFloorNumber(2);
//        floor2.setFloorName("Second Floor");
//        floor2.setIsActive(true);
//        floor2 = floorRepository.save(floor2);
//        log.info("   ✓ Created: {} - Floor {} (ID: {})",
//                building1.getName(), floor2.getFloorNumber(), floor2.getId());
//
//        Floor floor3 = new Floor();
//        floor3.setBuilding(building1);
//        floor3.setFloorNumber(3);
//        floor3.setFloorName("Third Floor");
//        floor3.setIsActive(true);
//        floor3 = floorRepository.save(floor3);
//        log.info("   ✓ Created: {} - Floor {} (ID: {})",
//                building1.getName(), floor3.getFloorNumber(), floor3.getId());
//
//        log.info("✅ Floors seeded: 3 floors created for {}", building1.getName());
//        return floor1;
//    }
//
//    private Floor getSecondFloor(Building building) {
//        return floorRepository.findByBuildingIdAndFloorNumber(building.getId(), 2)
//                .orElseThrow(() -> new RuntimeException("Second floor not found"));
//    }
//
//    private Floor getThirdFloor(Building building) {
//        return floorRepository.findByBuildingIdAndFloorNumber(building.getId(), 3)
//                .orElseThrow(() -> new RuntimeException("Third floor not found"));
//    }
//
//    // ========================================================================
//    // SEED RESOURCES
//    // ========================================================================
//    private void seedResources(Floor floor1, Department existingDept) {
//        log.info("📦 Seeding resources on floor: {} (Department: {})...",
//                floor1.getFloorName(), existingDept.getName());
//
//        int resourceCount = 0;
//
//        // ========================================================================
//        // Conference Rooms
//        // ========================================================================
//        Resource confRoom1 = new Resource();
//        confRoom1.setFloor(floor1);
//        confRoom1.setOwningDepartment(existingDept);
//        confRoom1.setResourceType(ResourceType.ROOM);
//        confRoom1.setName("Conference Room A");
//        confRoom1.setCapacity(10);
//        confRoom1.setDescription("Large conference room with projector and whiteboard");
//        confRoom1.setIsActive(true);
//        confRoom1 = resourceRepository.save(confRoom1);
//        resourceCount++;
//        log.info("   ✓ Created: {} (Capacity: {}, ID: {})",
//                confRoom1.getName(), confRoom1.getCapacity(), confRoom1.getId());
//
//        Resource confRoom2 = new Resource();
//        confRoom2.setFloor(floor1);
//        confRoom2.setOwningDepartment(existingDept);
//        confRoom2.setResourceType(ResourceType.ROOM);
//        confRoom2.setName("Conference Room B");
//        confRoom2.setCapacity(6);
//        confRoom2.setDescription("Medium conference room with video conferencing");
//        confRoom2.setIsActive(true);
//        confRoom2 = resourceRepository.save(confRoom2);
//        resourceCount++;
//        log.info("   ✓ Created: {} (Capacity: {}, ID: {})",
//                confRoom2.getName(), confRoom2.getCapacity(), confRoom2.getId());
//
//        Resource huddle = new Resource();
//        huddle.setFloor(floor1);
//        huddle.setOwningDepartment(existingDept);
//        huddle.setResourceType(ResourceType.ROOM);
//        huddle.setName("Huddle Room 1");
//        huddle.setCapacity(4);
//        huddle.setDescription("Small huddle room for quick meetings");
//        huddle.setIsActive(true);
//        huddle = resourceRepository.save(huddle);
//        resourceCount++;
//        log.info("   ✓ Created: {} (Capacity: {}, ID: {})",
//                huddle.getName(), huddle.getCapacity(), huddle.getId());
//
//        log.info("   📍 Rooms created: 3");
//
//        // ========================================================================
//        // Hot Desks
//        // ========================================================================
//        for (int i = 1; i <= 5; i++) {
//            Resource desk = new Resource();
//            desk.setFloor(floor1);
//            desk.setOwningDepartment(existingDept);
//            desk.setResourceType(ResourceType.DESK);
//            desk.setName("Hot Desk " + i);
//            desk.setCapacity(null); // Desks don't have capacity
//            desk.setDescription("Available hot desk with monitor and docking station");
//            desk.setIsActive(true);
//            desk = resourceRepository.save(desk);
//            resourceCount++;
//
//            if (i == 1 || i == 5) { // Only log first and last
//                log.info("   ✓ Created: {} (ID: {})", desk.getName(), desk.getId());
//            }
//        }
//        log.info("   🖥️ Desks created: 5");
//
//        // ========================================================================
//        // Parking Spaces
//        // ========================================================================
//        for (int i = 1; i <= 3; i++) {
//            Resource parking = new Resource();
//            parking.setFloor(floor1);
//            parking.setOwningDepartment(existingDept);
//            parking.setResourceType(ResourceType.PARKING);
//            parking.setName("Parking Space " + i);
//            parking.setCapacity(null);
//            parking.setDescription("Covered parking space");
//            parking.setIsActive(true);
//            parking = resourceRepository.save(parking);
//            resourceCount++;
//            log.info("   ✓ Created: {} (ID: {})", parking.getName(), parking.getId());
//        }
//        log.info("   🚗 Parking spaces created: 3");
//
//        log.info("✅ Resources seeded: {} total resources created", resourceCount);
//        log.info("   - 3 Conference Rooms (capacities: 10, 6, 4)");
//        log.info("   - 5 Hot Desks");
//        log.info("   - 3 Parking Spaces");
//    }
//}
