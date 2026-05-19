package com.transitpulse.notification.controller;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.notification.dto.NotificationResponse;
import com.transitpulse.notification.dto.UnreadCountResponse;
import com.transitpulse.notification.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getAll(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationService.getAll(currentUser);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return notificationService.getUnreadCount(currentUser);
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return notificationService.markAsRead(id, currentUser);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        notificationService.markAllAsRead(currentUser);
    }
}
