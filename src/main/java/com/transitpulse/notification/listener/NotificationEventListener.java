package com.transitpulse.notification.listener;

import com.transitpulse.notification.service.NotificationService;
import com.transitpulse.report.event.ReportVerifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onReportVerified(ReportVerifiedEvent event) {
        notificationService.createForVerifiedReport(event);
    }
}
