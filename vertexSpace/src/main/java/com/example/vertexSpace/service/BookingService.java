package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.request.CreateBookingRequest;
import com.example.vertexSpace.dto.request.CreateRecurringBookingRequest;
import com.example.vertexSpace.dto.response.*;

import java.time.Instant;
import java.util.UUID;


public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, UUID currentUserId);

    BookingResponse getBookingById(UUID bookingId, UUID currentUserId);
    BookingListResponse getUserBookings(UUID userId);
    BookingListResponse getUserUpcomingBookings(UUID userId);
    BookingListResponse getUserPastBookings(UUID userId);
    BookingListResponse getResourceBookings(UUID resourceId, UUID currentUserId);
    BookingListResponse getResourceBookingsInRange(
            UUID resourceId,
            Instant startTime,
            Instant endTime,
            UUID currentUserId
    );

    BookingCancellationResponse cancelBooking(UUID bookingId, UUID currentUserId);
    BookingListResponse getAllBookings(UUID currentUserId);
    BookingListResponse getDepartmentBookings(UUID departmentId, UUID currentUserId);
    RecurringBookingResponse createRecurringBooking(
            CreateRecurringBookingRequest request,
            UUID currentUserId
    );

    BookingListResponse getRecurringSeriesBookings(UUID seriesId, UUID currentUserId);
    RecurringSeriesCancellationResponse cancelRecurringSeries(UUID seriesId, UUID currentUserId);

}
