package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.request.CreateBookingRequest;
import com.example.vertexSpace.dto.request.CreateRecurringBookingRequest;
import com.example.vertexSpace.dto.response.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for Booking operations
 * ALL users can create/view/cancel own bookings
 * DEPT_ADMIN: Can view bookings for resources in own department
 * SYSTEM_ADMIN: Can view all bookings
 */
public interface BookingService {

    /**
     * Create a new booking
     * @param request Booking creation request
     * @param currentUserId Current user's ID
     * @return Created booking response
     */
    BookingResponse createBooking(CreateBookingRequest request, UUID currentUserId);

    /**
     * Get booking by ID
     * @param bookingId Booking ID
     * @param currentUserId Current user's ID (for authorization)
     * @return Booking response
     */
    BookingResponse getBookingById(UUID bookingId, UUID currentUserId);

    /**
     * Get all bookings for current user
     * @param userId User ID
     * @return List of user's bookings
     */
    BookingListResponse getUserBookings(UUID userId);

    /**
     * Get user's upcoming bookings
     * @param userId User ID
     * @return List of future bookings
     */
    BookingListResponse getUserUpcomingBookings(UUID userId);

    /**
     * Get user's past bookings
     * @param userId User ID
     * @return List of past bookings
     */
    BookingListResponse getUserPastBookings(UUID userId);

    /**
     * Get bookings for a specific resource
     * @param resourceId Resource ID
     * @param currentUserId Current user's ID (for authorization)
     * @return List of resource bookings
     */
    BookingListResponse getResourceBookings(UUID resourceId, UUID currentUserId);

    /**
     * Get bookings in date range for resource
     * @param resourceId Resource ID
     * @param startTime Range start time
     * @param endTime Range end time
     * @param currentUserId Current user's ID (for authorization)
     * @return List of bookings in range
     */
    BookingListResponse getResourceBookingsInRange(
            UUID resourceId,
            Instant startTime,
            Instant endTime,
            UUID currentUserId
    );

    /**
     * Cancel booking
     * @param bookingId Booking ID
     * @param currentUserId Current user's ID (for authorization)
     * @return Cancellation confirmation
     */
    BookingCancellationResponse cancelBooking(UUID bookingId, UUID currentUserId);

    /**
     * Get all bookings (SYSTEM_ADMIN only)
     * @param currentUserId Current user's ID (for authorization)
     * @return List of all bookings
     */
    BookingListResponse getAllBookings(UUID currentUserId);

    /**
     * Get department bookings (DEPT_ADMIN view)
     * @param departmentId Department ID
     * @param currentUserId Current user's ID (for authorization)
     * @return List of bookings for department's resources
     */
    BookingListResponse getDepartmentBookings(UUID departmentId, UUID currentUserId);
    /**
     * Create recurring booking with partial-conflict handling
     */
    RecurringBookingResponse createRecurringBooking(
            CreateRecurringBookingRequest request,
            UUID currentUserId
    );

    /**
     * Get all bookings in a recurring series
     */
    BookingListResponse getRecurringSeriesBookings(UUID seriesId, UUID currentUserId);

    /**
     * Cancel entire recurring series (all future occurrences)
     */
    RecurringSeriesCancellationResponse cancelRecurringSeries(UUID seriesId, UUID currentUserId);

}
