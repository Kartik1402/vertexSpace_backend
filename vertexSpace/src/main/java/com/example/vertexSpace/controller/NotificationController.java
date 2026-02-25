package com.example.vertexSpace.controller;

import com.example.vertexSpace.dto.notification.NotificationResponseDTO;
import com.example.vertexSpace.dto.notification.NotificationSummaryDTO;
import com.example.vertexSpace.service.AuthService;
import com.example.vertexSpace.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return authService.getUserIdByEmail(authentication.getName());
    }

    @GetMapping
    @Operation(
            summary = "Get my notifications",
            description = "Get paginated list of notifications. Valid sort fields: createdAt, priority, status"
    )
    public ResponseEntity<Page<NotificationResponseDTO>> getMyNotifications(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (createdAt, priority, status)") @RequestParam(required = false) String sort,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);

        // ✅ Create pageable with validated sort
        Pageable pageable = createValidatedPageable(page, size, sort, direction);

        Page<NotificationResponseDTO> notifications = notificationService.getMyNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get notification summary", description = "Get unread count and recent notifications")
    public ResponseEntity<NotificationSummaryDTO> getNotificationSummary(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        NotificationSummaryDTO summary = notificationService.getNotificationSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ✅ Helper method to validate and create Pageable
    private Pageable createValidatedPageable(int page, int size, String sort, String direction) {
        // Limit size to prevent abuse
        size = Math.min(size, 100);

        // Default sort
        if (sort == null || sort.trim().isEmpty() || sort.equalsIgnoreCase("string")) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // Validate sort field (only allow specific fields)
        String sortField = validateSortField(sort);

        // Determine direction
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortField));
    }

    // ✅ Whitelist valid sort fields
    private String validateSortField(String sort) {
        return switch (sort.toLowerCase()) {
            case "createdat", "created_at" -> "createdAt";
            case "priority" -> "priority";
            case "status" -> "status";
            case "type" -> "type";
            case "readat", "read_at" -> "readAt";
            default -> "createdAt"; // Default to createdAt if invalid
        };
    }
}
