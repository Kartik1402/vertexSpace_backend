package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.dto.waitlist.WaitlistEntryResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response when booking conflict results in automatic waitlist entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWithWaitlistResponse {

    /**
     * The pending booking that was created
     */
    private BookingResponse pendingBooking;

    /**
     * The waitlist entry that was created
     */
    private WaitlistEntryResponseDTO waitlistEntry;

    /**
     * Conflicting bookings
     */
    private List<BookingConflictResponseDTO.ConflictingBooking> conflicts;

    /**
     * Alternative available resources
     */
    private List<ResourceResponse> alternativeResources;

    /**
     * Suggested alternative time slots
     */
    private List<BookingConflictResponseDTO.TimeSlotSuggestion> suggestedTimeSlots;

    /**
     * User-friendly message
     */
    private String message;

}
