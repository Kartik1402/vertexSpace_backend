package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of bookings with metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingListResponse {

    private List<BookingResponse> bookings;
    private Long totalCount;
    private Integer upcomingCount;
    private Integer pastCount;
}
