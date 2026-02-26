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
    // Create a notification for a user

    Notification createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata
    );

    //Create a high-priority notification

    Notification createUrgentNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata,
            UUID relatedEntityId,
            String relatedEntityType
    );


    Page<NotificationResponseDTO> getMyNotifications(UUID userId, Pageable pageable);

    //Get notification summary (unread count + recent)
    NotificationSummaryDTO getNotificationSummary(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);

    int markAllAsRead(UUID userId);
    void deleteNotification(UUID notificationId, UUID userId);
    void cleanupOldNotifications();
    void notifyWaitlistOffer(UUID userId, UUID offerId, String resourceName, String timeSlot, int minutesRemaining);

    void notifyBookingCancelled(UUID userId, UUID bookingId, String resourceName, String timeSlot);
    void notifyBookingConfirmed(UUID userId, UUID bookingId, String resourceName, String timeSlot);

    void notifyBookingReminder(UUID userId, UUID bookingId, String resourceName, String timeSlot, int minutesUntil);
    void notifyAddedToWaitlist(UUID userId, UUID waitlistEntryId, String resourceName, int queuePosition);
}
