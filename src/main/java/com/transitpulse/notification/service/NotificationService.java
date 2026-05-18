package com.transitpulse.notification.service;

import com.transitpulse.notification.entity.Notification;
import com.transitpulse.notification.repository.NotificationRepository;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
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

    @Transactional
    public void createForVerifiedReport(ReportVerifiedEvent event) {
        Report report = reportRepository.findById(event.reportId())
                .orElseThrow(() -> new IllegalStateException("Verified report not found: " + event.reportId()));
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
