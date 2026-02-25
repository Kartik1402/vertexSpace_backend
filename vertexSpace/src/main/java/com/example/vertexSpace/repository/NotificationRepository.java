package com.example.vertexSpace.repository;

import com.example.vertexSpace.entity.Notification;
import com.example.vertexSpace.enums.NotificationStatus;
import com.example.vertexSpace.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a user
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
        AND (n.showAtUtc IS NULL OR n.showAtUtc <= :now)
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        ORDER BY n.priority DESC, n.createdAt DESC
        """)
    Page<Notification> findByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * Find unread notifications for a user
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
        AND n.status = 'UNREAD'
        AND (n.showAtUtc IS NULL OR n.showAtUtc <= :now)
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        ORDER BY n.priority DESC, n.createdAt DESC
        """)
    List<Notification> findUnreadByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Count unread notifications
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.user.id = :userId
        AND n.status = 'UNREAD'
        AND (n.showAtUtc IS NULL OR n.showAtUtc <= :now)
        AND (n.expiresAt IS NULL OR n.expiresAt > :now)
        """)
    long countUnreadByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    /**
     * Mark all as read for a user
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.status = 'READ', n.readAt = :readAt
        WHERE n.user.id = :userId
        AND n.status = 'UNREAD'
        """)
    int markAllAsReadByUserId(
            @Param("userId") UUID userId,
            @Param("readAt") Instant readAt
    );

    /**
     * Delete old notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find expired notifications to clean up
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.expiresAt IS NOT NULL
        AND n.expiresAt < :now
        """)
    List<Notification> findExpiredNotifications(@Param("now") Instant now);
}
