package com.transitpulse.notification.service;

import com.transitpulse.notification.entity.Notification;
import com.transitpulse.notification.dto.NotificationResponse;
import com.transitpulse.notification.dto.UnreadCountResponse;
import com.transitpulse.notification.exception.NotificationNotFoundException;
import com.transitpulse.notification.mapper.NotificationMapper;
import com.transitpulse.notification.repository.NotificationRepository;
import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.exception.ReportNotFoundException;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String AUTHOR_TITLE = "Your report has been verified";
    private static final String OTHER_USERS_TITLE = "New verified report";

    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(AuthenticatedUser currentUser) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.id()).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(AuthenticatedUser currentUser) {
        return new UnreadCountResponse(
                notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.id())
        );
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, AuthenticatedUser currentUser) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.id())
                .orElseThrow(NotificationNotFoundException::new);

        notification.markAsRead(Instant.now());

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(AuthenticatedUser currentUser) {
        Instant readAt = Instant.now();
        notificationRepository.findByRecipientIdAndReadAtIsNull(currentUser.id())
                .forEach(notification -> notification.markAsRead(readAt));
    }

    @Transactional
    public void createForVerifiedReport(ReportVerifiedEvent event) {
        Report report = reportRepository.findById(event.reportId())
                .orElseThrow(ReportNotFoundException::new);
        List<User> users = userRepository.findAll();

        List<Notification> notifications = users.stream()
                .map(user -> createNotification(user, report, event))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    private Notification createNotification(User recipient, Report report, ReportVerifiedEvent event) {
        boolean isAuthor = recipient.getId().equals(event.authorId());
        String title = isAuthor ? AUTHOR_TITLE : OTHER_USERS_TITLE;
        String message = buildMessage(isAuthor, event);

        return new Notification(recipient, report, title, message);
    }

    private String buildMessage(boolean isAuthor, ReportVerifiedEvent event) {
        String subject = buildReportSubject(event);

        if (isAuthor) {
            return "Your report " + subject + " has been verified.";
        }

        return "A report " + subject + " has been verified.";
    }

    private String buildReportSubject(ReportVerifiedEvent event) {
        if (hasText(event.lineNumber()) && hasText(event.stopName())) {
            return "for line " + event.lineNumber() + " near " + event.stopName();
        }

        if (hasText(event.lineNumber())) {
            return "for line " + event.lineNumber();
        }

        if (hasText(event.stopName())) {
            return "for stop " + event.stopName();
        }

        return "of type " + event.type();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
