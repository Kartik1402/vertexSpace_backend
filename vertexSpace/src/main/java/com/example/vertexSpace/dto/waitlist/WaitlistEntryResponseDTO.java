package com.example.vertexSpace.dto.waitlist;

import com.example.vertexSpace.dto.response.WaitlistEntryResponse;
import com.example.vertexSpace.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for waitlist entry
 *
 * Contains all information about a user's position in the waitlist,
 * including pending booking reference and offer details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaitlistEntryResponseDTO  {

    /**
     * Waitlist entry ID
     */
    private UUID id;

    /**
     * Resource information
     */
    private UUID resourceId;
    private String resourceName;
    private ResourceType resourceType;

    /**
     * User information
     */
    private UUID userId;
    private String userEmail;
    private String userDisplayName;

    /**
     * Reference to pending booking
     * This booking is created when user joins waitlist (status = PENDING_WAITLIST)
     * Converts to CONFIRMED when offer is accepted
     */
    private UUID pendingBookingId;

    /**
     * Requested time slot
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant startUtc;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant endUtc;

    /**
     * Purpose of the booking
     */
    private String purpose;

    /**
     * Current status
     * ACTIVE | OFFERED | FULFILLED | DECLINED | EXPIRED | CANCELLED
     */
    private String status;

    /**
     * Position in queue (1 = next in line)
     * NULL when no longer in queue (fulfilled/declined/expired/cancelled)
     */
    private Integer queuePosition;

    /**
     * When offer expires (only set when status = OFFERED)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant offerExpiresAt;

    /**
     * Minutes until offer expires (calculated field)
     */
    private Long minutesUntilExpiry;

    /**
     * Timestamps
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant offeredAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant fulfilledAt;

    /**
     * User-friendly status message
     * Examples:
     * - "You're #3 in the queue"
     * - "Offer expires in 12 minutes"
     * - "Offer accepted - booking confirmed"
     */
    private String message;

    /**
     * Additional metadata for frontend
     */
    private Boolean canAccept;  // Can user accept this offer right now?
    private Boolean canDecline; // Can user decline this offer right now?
    private Boolean canCancel;  // Can user cancel this waitlist entry?

    /**
     * Calculate action availability based on status
     */
    public void calculateActions() {
        this.canAccept = "OFFERED".equals(status);
        this.canDecline = "OFFERED".equals(status);
        this.canCancel = "ACTIVE".equals(status) || "OFFERED".equals(status);
    }
}
