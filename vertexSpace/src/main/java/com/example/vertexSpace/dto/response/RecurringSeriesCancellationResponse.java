package com.example.vertexSpace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringSeriesCancellationResponse {
    private UUID seriesId;
    private Integer cancelledCount;
    private List<UUID> cancelledBookingIds;
    private String message;
    private Instant cancelledAt;
}
