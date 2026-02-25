package com.example.vertexSpace.dto.notification;

import com.example.vertexSpace.enums.NotificationStatus;
import com.example.vertexSpace.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationStatus status;
    private Integer priority;
    private Map<String, String> metadata;
    private UUID relatedEntityId;
    private String relatedEntityType;
    private Instant createdAt;
    private Instant readAt;
    private Instant expiresAt;
    private boolean isExpired;
    private boolean isHighPriority;
}
