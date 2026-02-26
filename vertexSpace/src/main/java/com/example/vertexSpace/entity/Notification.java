package com.example.vertexSpace.entity;

import com.example.vertexSpace.enums.NotificationStatus;
import com.example.vertexSpace.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_status", columnList = "user_id, status"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;
    @ElementCollection(fetch = FetchType.EAGER)  // ✅ ADD THIS
    @CollectionTable(name = "notification_metadata", joinColumns = @JoinColumn(name = "notification_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "show_at_utc")
    private Instant showAtUtc;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Helper methods
    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isHighPriority() {
        return priority >= 3;
    }
}
