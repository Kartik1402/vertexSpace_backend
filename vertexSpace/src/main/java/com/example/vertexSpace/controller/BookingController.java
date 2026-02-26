package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.request.CreateBookingRequest;
import com.example.vertexSpace.dto.request.CreateRecurringBookingRequest;
import com.example.vertexSpace.dto.response.*;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * REST Controller for Booking management
 **/
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Booking management APIs")
@SecurityRequirement(name = "bearer-auth")
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking for a resource")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        String currentUserEmail = authentication.getName();
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);

        log.info("REST: Creating booking for resource {} by user {}", request.getResourceId(), currentUserId);
        BookingResponse response = bookingService.createBooking(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Retrieve booking details by ID")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable("id") UUID bookingId,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);

        log.info("REST: Fetching booking: {} by user {}", bookingId, currentUserId);
        BookingResponse response = bookingService.getBookingById(bookingId, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's bookings
     * GET /api/v1/bookings/my-bookings
     * Role: All authenticated users
     */
    @GetMapping("/my-bookings")
    @Operation(summary = "Get my bookings", description = "Retrieve all bookings for current user")
    public ResponseEntity<BookingListResponse> getMyBookings(Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Fetching bookings for user: {}", currentUserId);
        BookingListResponse response = bookingService.getUserBookings(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's upcoming bookings
     * GET /api/v1/bookings/my-bookings/upcoming
     * Role: All authenticated users
     */
    @GetMapping("/my-bookings/upcoming")
    @Operation(summary = "Get my upcoming bookings", description = "Retrieve future bookings for current user")
    public ResponseEntity<BookingListResponse> getMyUpcomingBookings(Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);

        log.info("REST: Fetching upcoming bookings for user: {}", currentUserId);
        BookingListResponse response = bookingService.getUserUpcomingBookings(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's past bookings
     * GET /api/v1/bookings/my-bookings/past
     * Role: All authenticated users
     */
    @GetMapping("/my-bookings/past")
    @Operation(summary = "Get my past bookings", description = "Retrieve past bookings for current user")
    public ResponseEntity<BookingListResponse> getMyPastBookings(Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Fetching past bookings for user: {}", currentUserId);
        BookingListResponse response = bookingService.getUserPastBookings(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings for specific user (admin view)
     * GET /api/v1/bookings/user/{userId}
     * Role: SYSTEM_ADMIN only
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('DEPT_ADMIN')")
    @Operation(summary = "Get user bookings", description = "Retrieve bookings for specific user (SYSTEM_ADMIN only)")
    public ResponseEntity<BookingListResponse> getUserBookings(@PathVariable("userId") UUID userId) {
        log.info("REST: Fetching bookings for user: {}", userId);
        BookingListResponse response = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings for specific resource
     * GET /api/v1/bookings/resource/{resourceId}
     * Role: DEPT_ADMIN (if in department) or SYSTEM_ADMIN
     */
    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Get resource bookings", description = "Retrieve all bookings for a resource")
    public ResponseEntity<BookingListResponse> getResourceBookings(
            @PathVariable("resourceId") UUID resourceId,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Fetching bookings for resource: {} by user {}", resourceId, currentUserId);
        BookingListResponse response = bookingService.getResourceBookings(resourceId, currentUserId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/resource/{resourceId}/range")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Get resource bookings in range", description = "Retrieve bookings for resource in date range")
    public ResponseEntity<BookingListResponse> getResourceBookingsInRange(
            @PathVariable("resourceId") UUID resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Fetching bookings for resource {} from {} to {} by user {}",
                resourceId, startTime, endTime, currentUserId);
        BookingListResponse response = bookingService.getResourceBookingsInRange(
                resourceId, startTime, endTime, currentUserId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get all bookings (system-wide)
     * GET /api/v1/bookings/all
     * Role: SYSTEM_ADMIN only
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Get all bookings", description = "Retrieve all bookings system-wide (SYSTEM_ADMIN only)")
    public ResponseEntity<BookingListResponse> getAllBookings(Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Fetching all bookings (admin view) by user {}", currentUserId);
        BookingListResponse response = bookingService.getAllBookings(currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get bookings for department
     * GET /api/v1/bookings/department/{departmentId}
     * Role: DEPT_ADMIN (own department) or SYSTEM_ADMIN
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DEPT_ADMIN')")
    @Operation(summary = "Get department bookings", description = "Retrieve bookings for department's resources")
    public ResponseEntity<BookingListResponse> getDepartmentBookings(
            @PathVariable("departmentId") UUID departmentId,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);        log.info("REST: Fetching bookings for department {} by user {}", departmentId, currentUserId);
        BookingListResponse response = bookingService.getDepartmentBookings(departmentId, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel booking
     * DELETE /api/v1/bookings/{id}
     * Role: Owner or SYSTEM_ADMIN
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel booking", description = "Cancel a booking (owner or SYSTEM_ADMIN)")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(
            @PathVariable("id") UUID bookingId,
            Authentication authentication) {
        String currentUserEmail = authentication.getName(); // e.g., kartik50@gmail.com
        UUID currentUserId =  authService.getUserIdByEmail(currentUserEmail);
        log.info("REST: Cancelling booking: {} by user {}", bookingId, currentUserId);
        BookingCancellationResponse response = bookingService.cancelBooking(bookingId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recurring")
    @Operation(
            summary = "Create recurring booking",
            description = "Create multiple bookings based on recurrence pattern (weekly, daily, monthly). " +
                    "Handles conflicts by skipping unavailable dates or failing entire series based on strategy."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recurring bookings created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or all occurrences have conflicts"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Conflict resolution strategy rejected series")
    })
    public ResponseEntity<RecurringBookingResponse> createRecurringBooking(
            @Valid @RequestBody CreateRecurringBookingRequest request,
            Authentication authentication) {

        UUID userId = authService.getUserIdByEmail(authentication.getName());
        RecurringBookingResponse response = bookingService.createRecurringBooking(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all bookings in a recurring series
     */
    @GetMapping("/recurring/{seriesId}")
    @Operation(
            summary = "Get recurring series bookings",
            description = "Get all bookings (past and future) in a recurring series"
    )
    public ResponseEntity<BookingListResponse> getRecurringSeriesBookings(
            @PathVariable UUID seriesId,
            Authentication authentication) {

        UUID userId = authService.getUserIdByEmail(authentication.getName());
        BookingListResponse response = bookingService.getRecurringSeriesBookings(seriesId, userId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/recurring/{seriesId}")
    @Operation(
            summary = "Cancel recurring series",
            description = "Cancel all future bookings in a recurring series. Past bookings are not affected."
    )
    public ResponseEntity<RecurringSeriesCancellationResponse> cancelRecurringSeries(
            @PathVariable UUID seriesId,
            Authentication authentication) {

        UUID userId = authService.getUserIdByEmail(authentication.getName());
        RecurringSeriesCancellationResponse response = bookingService.cancelRecurringSeries(seriesId, userId);

        return ResponseEntity.ok(response);
    }

}
