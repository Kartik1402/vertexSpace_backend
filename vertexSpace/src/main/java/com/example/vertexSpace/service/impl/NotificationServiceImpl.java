package com.example.vertexSpace.service.impl;

import com.example.vertexSpace.dto.notification.NotificationResponseDTO;
import com.example.vertexSpace.dto.notification.NotificationSummaryDTO;
import com.example.vertexSpace.entity.Notification;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.enums.NotificationStatus;
import com.example.vertexSpace.enums.NotificationType;
import com.example.vertexSpace.exception.AuthorizationException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.NotificationRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public Notification createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .status(NotificationStatus.UNREAD)
                .priority(determinePriority(type))
                .build();

        notification = notificationRepo.save(notification);

        log.info("Created notification {} for user {}: {}", notification.getId(), userId, title);

        // TODO: Send real-time notification via WebSocket
        // webSocketService.sendNotification(userId, notification);

        return notification;
    }

    @Override
    @Transactional
    public Notification createUrgentNotification(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> metadata,
            UUID relatedEntityId,
            String relatedEntityType) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .status(NotificationStatus.UNREAD)
                .priority(4) // Urgent
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .expiresAt(Instant.now().plus(Duration.ofHours(24))) // Expires in 24 hours
                .build();

        notification = notificationRepo.save(notification);

        log.info("Created URGENT notification {} for user {}", notification.getId(), userId);

        // TODO: Send push notification, email, etc.
        // pushNotificationService.send(userId, title, message);

        return notification;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getMyNotifications(UUID userId, Pageable pageable) {
        Instant now = Instant.now();
        Page<Notification> notifications = notificationRepo.findByUserId(userId, now, pageable);

        return notifications.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryDTO getNotificationSummary(UUID userId) {
        Instant now = Instant.now();

        long unreadCount = notificationRepo.countUnreadByUserId(userId, now);

        List<Notification> recent = notificationRepo.findUnreadByUserId(userId, now);

        // Limit to 5 most recent
        List<NotificationResponseDTO> recentDTOs = recent.stream()
                .limit(5)
                .map(this::toDTO)
                .collect(Collectors.toList());

        return NotificationSummaryDTO.builder()
                .unreadCount(unreadCount)
                .totalCount(notificationRepo.countUnreadByUserId(userId, now))
                .recentNotifications(recentDTOs)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AuthorizationException("Not your notification");
        }

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.markAsRead();
            notificationRepo.save(notification);
            log.info("Marked notification {} as read", notificationId);
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepo.markAllAsReadByUserId(userId, Instant.now());
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new AuthorizationException("Not your notification");
        }

        notificationRepo.delete(notification);
        log.info("Deleted notification {}", notificationId);
    }

    @Override
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Running notification cleanup job");

        // Delete notifications older than 90 days
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        int deleted = notificationRepo.deleteOlderThan(cutoff);

        log.info("Deleted {} old notifications", deleted);

        // Delete expired notifications
        List<Notification> expired = notificationRepo.findExpiredNotifications(Instant.now());
        if (!expired.isEmpty()) {
            notificationRepo.deleteAll(expired);
            log.info("Deleted {} expired notifications", expired.size());
        }
    }

    // ===================================================================
    // Domain-specific notifications
    // ===================================================================

    @Override
    public void notifyWaitlistOffer(UUID userId, UUID offerId, String resourceName, String timeSlot, int minutesRemaining) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("offerId", offerId.toString());
        metadata.put("resourceName", resourceName);
        metadata.put("timeSlot", timeSlot);
        metadata.put("minutesRemaining", String.valueOf(minutesRemaining));

        createUrgentNotification(
                userId,
                NotificationType.WAITLIST_OFFER_RECEIVED,
                "🎉 Your waitlist slot is ready!",
                String.format("You have %d minutes to accept the offer for %s at %s",
                        minutesRemaining, resourceName, timeSlot),
                metadata,
                offerId,
                "OFFER"
        );
    }

    @Override
    public void notifyBookingCancelled(UUID userId, UUID bookingId, String resourceName, String timeSlot) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId.toString());
        metadata.put("resourceName", resourceName);
        metadata.put("timeSlot", timeSlot);

        createNotification(
                userId,
                NotificationType.BOOKING_CANCELLED,
                "Booking Cancelled",
                String.format("Your booking for %s at %s has been cancelled", resourceName, timeSlot),
                metadata
        );
    }

    @Override
    public void notifyBookingConfirmed(UUID userId, UUID bookingId, String resourceName, String timeSlot) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId.toString());
        metadata.put("resourceName", resourceName);
        metadata.put("timeSlot", timeSlot);

        createNotification(
                userId,
                NotificationType.BOOKING_CONFIRMED,
                "✅ Booking Confirmed",
                String.format("Your booking for %s at %s is confirmed", resourceName, timeSlot),
                metadata
        );
    }

    @Override
    public void notifyBookingReminder(UUID userId, UUID bookingId, String resourceName, String timeSlot, int minutesUntil) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId.toString());
        metadata.put("resourceName", resourceName);
        metadata.put("timeSlot", timeSlot);
        metadata.put("minutesUntil", String.valueOf(minutesUntil));

        createNotification(
                userId,
                NotificationType.BOOKING_REMINDER,
                "⏰ Booking Reminder",
                String.format("Your booking for %s starts in %d minutes", resourceName, minutesUntil),
                metadata
        );
    }

    @Override
    public void notifyAddedToWaitlist(UUID userId, UUID waitlistEntryId, String resourceName, int queuePosition) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("waitlistEntryId", waitlistEntryId.toString());
        metadata.put("resourceName", resourceName);
        metadata.put("queuePosition", String.valueOf(queuePosition));

        createNotification(
                userId,
                NotificationType.WAITLIST_ADDED,
                "Added to Waitlist",
                String.format("You're #%d in line for %s", queuePosition, resourceName),
                metadata
        );
    }

    // ===================================================================
    // Helper methods
    // ===================================================================

    private int determinePriority(NotificationType type) {
        return switch (type) {
            case WAITLIST_OFFER_RECEIVED -> 4; // Urgent
            case BOOKING_REMINDER, WAITLIST_OFFER_EXPIRED -> 3; // High
            case BOOKING_CONFIRMED, BOOKING_CANCELLED -> 2; // Medium
            default -> 1; // Low
        };
    }

    private NotificationResponseDTO toDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .priority(notification.getPriority())
                .metadata(notification.getMetadata())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .expiresAt(notification.getExpiresAt())
                .isExpired(notification.isExpired())
                .isHighPriority(notification.isHighPriority())
                .build();
    }
}
