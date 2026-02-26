package com.example.vertexSpace.dto.response;

import com.example.vertexSpace.dto.waitlist.WaitlistEntryResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWithWaitlistResponse {

    private BookingResponse pendingBooking;
    private WaitlistEntryResponseDTO waitlistEntry;
    private List<BookingConflictResponseDTO.ConflictingBooking> conflicts;
    private List<ResourceResponse> alternativeResources;
    private List<BookingConflictResponseDTO.TimeSlotSuggestion> suggestedTimeSlots;
    private String message;

}
