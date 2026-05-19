package com.transitpulse.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.notification.dto.NotificationResponse;
import com.transitpulse.notification.dto.UnreadCountResponse;
import com.transitpulse.notification.entity.Notification;
import com.transitpulse.notification.exception.NotificationNotFoundException;
import com.transitpulse.notification.mapper.NotificationMapper;
import com.transitpulse.notification.repository.NotificationRepository;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.entity.ReportType;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.exception.ReportNotFoundException;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void unreadCountUsesCurrentUser() {
        User user = user(1L);
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(4L);

        UnreadCountResponse response = notificationService.getUnreadCount(AuthenticatedUser.from(user));

        assertEquals(4L, response.count());
    }

    @Test
    void markAsReadOnlyReadsNotificationOwnedByCurrentUser() {
        User user = user(1L);
        Report report = report(user, 10L);
        Notification notification = notification(user, report, 20L);
        NotificationResponse mapped = response(notification, true);

        when(notificationRepository.findByIdAndRecipientId(20L, 1L)).thenReturn(Optional.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(mapped);

        NotificationResponse response = notificationService.markAsRead(20L, AuthenticatedUser.from(user));

        assertNotNull(notification.getReadAt());
        assertEquals(mapped, response);
    }

    @Test
    void markAsReadThrowsWhenNotificationDoesNotBelongToCurrentUser() {
        User user = user(1L);
        when(notificationRepository.findByIdAndRecipientId(20L, 1L)).thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.markAsRead(20L, AuthenticatedUser.from(user))
        );
    }

    @Test
    void markAllAsReadMarksOnlyUnreadCurrentUserNotifications() {
        User user = user(1L);
        Report report = report(user, 10L);
        Notification first = notification(user, report, 20L);
        Notification second = notification(user, report, 21L);

        when(notificationRepository.findByRecipientIdAndReadAtIsNull(1L)).thenReturn(List.of(first, second));

        notificationService.markAllAsRead(AuthenticatedUser.from(user));

        assertNotNull(first.getReadAt());
        assertNotNull(second.getReadAt());
        assertEquals(first.getReadAt(), second.getReadAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void createForVerifiedReportCreatesNotificationForEveryUser() {
        User author = user(1L);
        User other = user(2L);
        Report report = report(author, 10L);
        ReportVerifiedEvent event = new ReportVerifiedEvent(
                10L,
                1L,
                ReportType.DELAY,
                "52",
                "Central",
                Instant.now()
        );

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(userRepository.findAll()).thenReturn(List.of(author, other));

        notificationService.createForVerifiedReport(event);

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> notifications = notificationsCaptor.getValue();

        assertEquals(2, notifications.size());
        assertEquals(author, notifications.get(0).getRecipient());
        assertEquals("Your report has been verified", notifications.get(0).getTitle());
        assertEquals("Your report for line 52 near Central has been verified.", notifications.get(0).getMessage());
        assertEquals(other, notifications.get(1).getRecipient());
        assertEquals("New verified report", notifications.get(1).getTitle());
        assertEquals("A report for line 52 near Central has been verified.", notifications.get(1).getMessage());
    }

    @Test
    void createForVerifiedReportThrowsWhenReportNoLongerExists() {
        ReportVerifiedEvent event = new ReportVerifiedEvent(
                10L,
                1L,
                ReportType.DELAY,
                null,
                null,
                Instant.now()
        );

        when(reportRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> notificationService.createForVerifiedReport(event));
    }

    private static Notification notification(User recipient, Report report, Long id) {
        Notification notification = new Notification(recipient, report, "Title", "Message");
        setId(notification, id);
        return notification;
    }

    private static NotificationResponse response(Notification notification, boolean read) {
        return new NotificationResponse(
                notification.getId(),
                notification.getReport().getId(),
                notification.getTitle(),
                notification.getMessage(),
                read,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private static Report report(User author, Long id) {
        Report report = new Report(author, ReportType.DELAY, "52", "Central", "Late tram", null);
        setId(report, id);
        return report;
    }

    private static User user(Long id) {
        User user = new User("user" + id + "@example.com", "password-hash", "User " + id, Role.USER);
        user.setId(id);
        return user;
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
