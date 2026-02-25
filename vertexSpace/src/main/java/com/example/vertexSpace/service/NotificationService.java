package com.example.vertexSpace.service;

import com.example.vertexSpace.dto.notification.NotificationResponseDTO;
import com.example.vertexSpace.dto.notification.NotificationSummaryDTO;
import com.example.vertexSpace.entity.Notification;
import com.example.vertexSpace.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    /**
     * Create a notification for a user
     */
    Notification createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata
    );

    /**
     * Create a high-priority notification
     */
    Notification createUrgentNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata,
            UUID relatedEntityId,
            String relatedEntityType
    );

    /**
     * Get all notifications for a user (paginated)
     */
    Page<NotificationResponseDTO> getMyNotifications(UUID userId, Pageable pageable);

    /**
     * Get notification summary (unread count + recent)
     */
    NotificationSummaryDTO getNotificationSummary(UUID userId);

    /**
     * Mark notification as read
     */
    void markAsRead(UUID notificationId, UUID userId);

    /**
     * Mark all notifications as read
     */
    int markAllAsRead(UUID userId);

    /**
     * Delete notification
     */
    void deleteNotification(UUID notificationId, UUID userId);

    /**
     * Cleanup old notifications (called by scheduler)
     */
    void cleanupOldNotifications();

    // ===================================================================
    // Domain-specific notification helpers
    // ===================================================================

    /**
     * Notify user about waitlist offer
     */
    void notifyWaitlistOffer(UUID userId, UUID offerId, String resourceName, String timeSlot, int minutesRemaining);

    /**
     * Notify user their booking was cancelled
     */
    void notifyBookingCancelled(UUID userId, UUID bookingId, String resourceName, String timeSlot);

    /**
     * Notify user their booking is confirmed
     */
    void notifyBookingConfirmed(UUID userId, UUID bookingId, String resourceName, String timeSlot);

    /**
     * Notify user about booking reminder
     */
    void notifyBookingReminder(UUID userId, UUID bookingId, String resourceName, String timeSlot, int minutesUntil);

    /**
     * Notify user they were added to waitlist
     */
    void notifyAddedToWaitlist(UUID userId, UUID waitlistEntryId, String resourceName, int queuePosition);
}
