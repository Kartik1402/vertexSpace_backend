package com.example.vertexSpace.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryDTO {
    private long totalCount;
    private long unreadCount;
    private List<NotificationResponseDTO> recentNotifications;
}
