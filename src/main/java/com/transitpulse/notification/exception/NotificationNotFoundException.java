package com.transitpulse.notification.exception;

import com.transitpulse.common.error.NotFoundException;

public class NotificationNotFoundException extends NotFoundException {

    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
