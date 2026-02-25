package com.example.vertexSpace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for VertexSpace Booking System
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access API docs at: http://localhost:8080/v3/api-docs
 *
 * HOW TO USE AUTHENTICATION:
 * 1. Login via POST /api/v1/auth/login (use employee@deloitte.com / Employee@123)
 * 2. Copy the "token" value from response
 * 3. Click "Authorize" button (🔓 padlock) at top of page
 * 4. Paste token (WITHOUT "Bearer " prefix) in the value field
 * 5. Click "Authorize" then "Close"
 * 6. All requests will now include your JWT token automatically
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:VertexSpace Booking System}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // ============================================================
                // API INFORMATION
                // ============================================================
                .info(new Info()
                        .title("VertexSpace Booking System API")
                        .version("3.0.0")  // Updated for Milestone 3
                        .description("""
                    # VertexSpace Resource Booking System - Milestone 3
                    
                    A comprehensive resource booking system with intelligent waitlist management and automated offer processing.
                    
                    ---
                    
                    ## 🎯 Milestone 3: Waitlist & Offers System
                    
                    ### ✨ NEW Features
                    
                    #### 📋 **Waitlist Management**
                    - **Join Waitlist**: Request notification when desired resource becomes available
                    - **FIFO Queue**: First-come, first-served fair allocation system
                    - **Smart Matching**: System finds best available slots automatically
                    - **Multi-Resource**: Support for rooms, desks, and parking spaces
                    - **Time-Aware**: Tracks preferred time slots with flexibility options
                    
                    #### 🎁 **Automated Offer System**
                    - **Instant Notifications**: Automatic offer creation when slots open up
                    - **10-Minute Window**: Accept or decline before offer expires
                    - **Priority Queue**: Next person gets offer if declined/expired
                    - **Conflict Prevention**: Ensures no double-bookings
                    - **Status Tracking**: Real-time offer status (PENDING → ACCEPTED/DECLINED/EXPIRED)
                    
                    #### ⚙️ **Background Processing**
                    - **Automatic Scheduling**: System processes waitlist queue every minute
                    - **Expiry Management**: Auto-expires old offers and moves to next in queue
                    - **Email Ready**: Framework for notification system (future enhancement)
                    
                    ---
                    
                    ## 🔄 Complete Waitlist Flow
                    
                    ### Scenario: Employee needs a meeting room
                    
                    **Step 1: Try to Book**
                    ```http
                    POST /api/v1/bookings
                    {
                      "resourceId": "room-uuid",
                      "startTimeUtc": "2025-02-23T10:00:00Z",
                      "endTimeUtc": "2025-02-23T11:00:00Z"
                    }
                    ```
                    **Response**: `409 Conflict` - Room already booked
                    
                    **Step 2: Join Waitlist**
                    ```http
                    POST /api/v1/waitlist-entries
                    {
                      "resourceId": "room-uuid",
                      "startTimeUtc": "2025-02-23T10:00:00Z",
                      "endTimeUtc": "2025-02-23T11:00:00Z",
                      "priority": "NORMAL"
                    }
                    ```
                    **Response**: `201 Created` - You're #3 in queue
                    
                    **Step 3: Another User Cancels**
                    ```
                    🔔 System detects cancellation → Processes waitlist queue
                    ```
                    
                    **Step 4: You Get an Offer**
                    ```http
                    GET /api/v1/me/waitlist-offers
                    ```
                    **Response**: Active offer with 10-minute deadline
                    ```json
                    {
                      "id": "offer-uuid",
                      "resourceName": "Conference Room A",
                      "startTimeUtc": "2025-02-23T10:00:00Z",
                      "endTimeUtc": "2025-02-23T11:00:00Z",
                      "status": "PENDING",
                      "expiresAtUtc": "2025-02-23T09:55:00Z"
                    }
                    ```
                    
                    **Step 5: Accept the Offer**
                    ```http
                    POST /api/v1/waitlist-offers/{offerId}/accept
                    ```
                    **Response**: `200 OK` - Booking confirmed! Waitlist entry marked FULFILLED.
                    
                    ---
                    
                    ## 📦 Milestone 2: Core Booking System
                    
                    ### Features
                    - **Building Management**: CRUD operations for buildings and floors
                    - **Resource Management**: Rooms, desks, parking spaces with capacity tracking
                    - **Booking System**: Time-based reservations with conflict detection
                    - **15-Minute Buffer**: Automatic cleanup time between bookings
                    - **Advanced Search**: Filter by type, location, capacity, availability
                    - **Role-Based Access**: SYSTEM_ADMIN, DEPT_ADMIN, EMPLOYEE permissions
                    
                    ---
                    
                    ## 🔐 Authentication Guide
                    
                    ### Quick Start
                    1. **Expand** the `Auth` section in Swagger
                    2. **Use** `POST /api/v1/auth/login`
                    3. **Login** with test credentials:
                       - **Employee**: `employee@deloitte.com` / `Employee@123`
                       - **Admin**: `admin@deloitte.com` / `Admin@123`
                    4. **Copy** the `token` value from the response (long string starting with `eyJ...`)
                    5. **Click** the **"Authorize"** button (🔓 padlock icon) at the top
                    6. **Paste** your token in the "Value" field
                       - ⚠️ **Important**: Paste ONLY the token, NOT "Bearer token"
                    7. **Click** "Authorize" then "Close"
                    8. **Test**: The padlock should now show 🔒 (locked)
                    
                    ### Verify It's Working
                    All your requests will now include:
                    ```
                    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                    ```
                    
                    Check the "Request headers" section after executing any endpoint to confirm.
                    
                    ---
                    
                    ## 👥 Roles & Permissions
                    
                    ### 🔴 SYSTEM_ADMIN
                    - ✅ Full system access
                    - ✅ View all waitlist entries and offers across all departments
                    - ✅ Manage all resources, bookings, buildings
                    - ✅ Override any reservation
                    
                    ### 🟡 DEPT_ADMIN
                    - ✅ Manage resources in own department
                    - ✅ View department waitlist and offers
                    - ✅ View department bookings and analytics
                    - ⛔ Cannot access other departments' data
                    
                    ### 🟢 EMPLOYEE (Standard User)
                    - ✅ Search and view all available resources
                    - ✅ Create and cancel own bookings
                    - ✅ Join/leave waitlist for any resource
                    - ✅ Accept/decline offers sent to them
                    - ✅ View own booking history
                    - ⛔ Cannot view other users' bookings
                    - ⛔ Cannot manage resources or buildings
                    
                    ---
                    
                    ## 🔍 API Endpoint Categories
                    
                    ### 🔑 Auth
                    - `POST /api/v1/auth/login` - Get JWT token
                    - `POST /api/v1/auth/register` - Create new account
                    
                    ### 🏢 Buildings & Floors
                    - Manage physical locations and floor plans
                    
                    ### 🪑 Resources
                    - Manage rooms, desks, parking spaces
                    - Search with filters (type, capacity, location, availability)
                    
                    ### 📅 Bookings
                    - `POST /api/v1/bookings` - Create booking
                    - `GET /api/v1/bookings/me` - View my bookings
                    - `DELETE /api/v1/bookings/{id}` - Cancel booking
                    
                    ### 📋 Waitlist
                    - `POST /api/v1/waitlist-entries` - Join waitlist
                    - `GET /api/v1/waitlist-entries/me` - View my waitlist entries
                    - `GET /api/v1/waitlist-entries` - View all entries (admin only)
                    - `DELETE /api/v1/waitlist-entries/{id}` - Leave waitlist
                    
                    ### 🎁 Waitlist Offers
                    - `GET /api/v1/me/waitlist-offers` - View my active offers
                    - `GET /api/v1/waitlist-offers/{id}` - Get offer details
                    - `POST /api/v1/waitlist-offers/{id}/accept` - Accept offer (creates booking)
                    - `POST /api/v1/waitlist-offers/{id}/decline` - Decline offer (moves to next)
                    
                    ### 🔍 Availability
                    - `GET /api/v1/availability/best-slots` - Find optimal time slots
                    - `GET /api/v1/availability/check` - Check specific time availability
                    
                    ---
                    
                    ## 📋 API Conventions
                    
                    ### Timestamps
                    - **Format**: ISO-8601 with UTC timezone
                    - **Example**: `2025-02-23T14:30:00Z`
                    - **Note**: Always use UTC. System converts to local time when needed.
                    
                    ### Identifiers
                    - **Type**: UUID (Universally Unique Identifier)
                    - **Format**: `550e8400-e29b-41d4-a716-446655440000`
                    - **Generation**: Auto-generated by database
                    
                    ### Soft Deletes
                    - Resources use `isActive` flag instead of physical deletion
                    - Inactive resources hidden from search but bookings remain intact
                    - Historical data preserved for analytics
                    
                    ### Buffer Time
                    - **Default**: 15 minutes between bookings
                    - **Purpose**: Cleanup, setup, transition time
                    - **Example**: Booking ends at 10:00 AM → Next booking starts at 10:15 AM
                    
                    ### Offer Expiry
                    - **Default**: 10 minutes (configurable)
                    - **Property**: `waitlist.offer-expiry-minutes=10`
                    - **Behavior**: Auto-declines and moves to next person in queue
                    
                    ---
                    
                    ## 🚀 Quick API Examples
                    
                    ### Example 1: Complete Booking Flow
                    ```bash
                    # 1. Login
                    POST /api/v1/auth/login
                    Body: { "email": "employee@deloitte.com", "password": "Employee@123" }
                    
                    # 2. Search available rooms
                    GET /api/v1/resources?type=ROOM&minCapacity=10
                    
                    # 3. Check specific time availability
                    GET /api/v1/availability/check?resourceIds=<uuid>&startUtc=2025-02-23T10:00:00Z&endUtc=2025-02-23T11:00:00Z
                    
                    # 4. Create booking
                    POST /api/v1/bookings
                    Body: {
                      "resourceId": "<uuid>",
                      "startTimeUtc": "2025-02-23T10:00:00Z",
                      "endTimeUtc": "2025-02-23T11:00:00Z",
                      "purpose": "Team Meeting"
                    }
                    ```
                    
                    ### Example 2: Waitlist Flow
                    ```bash
                    # 1. Join waitlist (if booking fails)
                    POST /api/v1/waitlist-entries
                    Body: {
                      "resourceId": "<uuid>",
                      "startTimeUtc": "2025-02-23T10:00:00Z",
                      "endTimeUtc": "2025-02-23T11:00:00Z",
                      "priority": "NORMAL"
                    }
                    
                    # 2. Check for offers periodically
                    GET /api/v1/me/waitlist-offers
                    
                    # 3. Accept offer when received
                    POST /api/v1/waitlist-offers/{offerId}/accept
                    ```
                    
                    ### Example 3: Find Best Available Slot
                    ```bash
                    # System suggests optimal times
                    GET /api/v1/availability/best-slots?resourceIds=<uuid>,<uuid>&startUtc=2025-02-23T09:00:00Z&endUtc=2025-02-23T17:00:00Z&duration=60
                    
                    # Returns: Top 5 available time slots sorted by preference
                    ```
                    
                    ---
                    
                    ## 🔧 Configuration
                    
                    ### Key Application Properties
                    ```properties
                    # Waitlist offer expiry time
                    waitlist.offer-expiry-minutes=10
                    
                    # Scheduler for processing expired offers
                    scheduler.expire-offers.cron=0 * * * * *
                    
                    # Default booking buffer time
                    booking.buffer-minutes=15
                    
                    # JWT token expiry
                    jwt.expiration-ms=86400000
                    ```
                    
                    ---
                    
                    ## ⚠️ Common Issues
                    
                    ### 403 Forbidden Error
                    **Cause**: Missing or invalid JWT token
                    **Fix**: Click "Authorize" button and paste your token
                    
                    ### 409 Conflict Error
                    **Cause**: Resource already booked for that time
                    **Fix**: Try different time or join waitlist
                    
                    ### 400 Bad Request
                    **Cause**: Invalid input (past dates, invalid UUID, etc.)
                    **Fix**: Check request body format and values
                    
                    ### Offer Already Expired
                    **Cause**: Took longer than 10 minutes to respond
                    **Fix**: Check for new offers - system moved to next person
                    
                    ---
                    
                    ## 📞 Support & Resources
                    
                    - **Technical Issues**: support@vertexspace.com
                    - **API Documentation**: http://localhost:8080/v3/api-docs
                    - **Swagger UI**: http://localhost:8080/swagger-ui.html
                    - **Development Team**: VertexSpace Engineering
                    
                    ---
                    
                    ## 📊 System Status Indicators
                    
                    ### Waitlist Entry Status
                    - `ACTIVE` - Waiting in queue
                    - `FULFILLED` - Got resource via offer acceptance
                    - `CANCELLED` - User manually left waitlist
                    - `EXPIRED` - Time slot passed without fulfillment
                    
                    ### Offer Status
                    - `PENDING` - Waiting for user response
                    - `ACCEPTED` - User accepted (booking created)
                    - `DECLINED` - User declined
                    - `EXPIRED` - 10 minutes passed without response
                    
                    ### Booking Status
                    - `CONFIRMED` - Active reservation
                    - `CANCELLED` - User cancelled
                    - `COMPLETED` - Time slot has passed
                    
                    ---
                    
                    **Version**: 3.0.0 | **Milestone**: 3 | **Last Updated**: February 2025
                    """)
                        .contact(new Contact()
                                .name("VertexSpace Development Team")
                                .email("support@vertexspace.com")
                                .url("https://vertexspace.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )

                // ============================================================
                // SERVER URLS
                // ============================================================
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api-dev.vertexspace.com")
                                .description("Development Server (if deployed)"),
                        new Server()
                                .url("https://api.vertexspace.com")
                                .description("Production Server (if applicable)")
                ))

                // ============================================================
                // SECURITY CONFIGURATION
                // ============================================================
                .components(new Components()
                        .addSecuritySchemes("bearer-auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("""
                                    JWT Authentication - Include your token in all authenticated requests.
                                    
                                    **How to authorize in Swagger:**
                                    
                                    1. Login via `POST /api/v1/auth/login`
                                    2. Copy the `token` from response (starts with `eyJ...`)
                                    3. Click the **"Authorize"** button (🔓 padlock icon) above
                                    4. Paste your token in the "Value" field
                                    5. Click "Authorize" then "Close"
                                    
                                    ⚠️ **Important**: Paste ONLY the token value, not "Bearer <token>"
                                    
                                    **Example token:**
                                    ```
                                    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJlbXBsb3llZUBkZWxvaXR0ZS5jb20iLCJpYXQiOjE3MDg1...
                                    ```
                                    
                                    **After authorization, all requests will include:**
                                    ```
                                    Authorization: Bearer <your-token>
                                    ```
                                    """)
                        )
                )

                // ============================================================
                // GLOBAL SECURITY REQUIREMENT
                // ============================================================
                .addSecurityItem(new SecurityRequirement().addList("bearer-auth"));
    }

    /**
     * Additional Swagger UI Configuration
     *
     * Add these to your application.properties or application.yml for enhanced UI:
     *
     * # Swagger UI Settings
     * springdoc.swagger-ui.path=/swagger-ui.html
     * springdoc.swagger-ui.operationsSorter=method
     * springdoc.swagger-ui.tagsSorter=alpha
     * springdoc.swagger-ui.tryItOutEnabled=true
     * springdoc.swagger-ui.filter=true
     * springdoc.swagger-ui.syntaxHighlight.activated=true
     * springdoc.swagger-ui.docExpansion=none
     * springdoc.swagger-ui.defaultModelsExpandDepth=1
     * springdoc.swagger-ui.displayRequestDuration=true
     *
     * # API Documentation
     * springdoc.api-docs.path=/v3/api-docs
     * springdoc.api-docs.enabled=true
     *
     * # Package Scanning
     * springdoc.packages-to-scan=com.example.vertexSpace.controller
     * springdoc.paths-to-match=/api/v1/**
     *
     * # Show/Hide Endpoints
     * springdoc.show-actuator=false
     *
     * # Group Endpoints by Feature
     * springdoc.group-configs[0].group=auth
     * springdoc.group-configs[0].paths-to-match=/api/v1/auth/**
     * springdoc.group-configs[1].group=bookings
     * springdoc.group-configs[1].paths-to-match=/api/v1/bookings/**
     * springdoc.group-configs[2].group=waitlist
     * springdoc.group-configs[2].paths-to-match=/api/v1/waitlist-entries/**,/api/v1/waitlist-offers/**
     * springdoc.group-configs[3].group=resources
     * springdoc.group-configs[3].paths-to-match=/api/v1/resources/**
     * springdoc.group-configs[4].group=availability
     * springdoc.group-configs[4].paths-to-match=/api/v1/availability/**
     */
}
