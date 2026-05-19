package com.transitpulse.report.exception;

import com.transitpulse.common.error.ConflictException;

public class ReportAlreadyConfirmedException extends ConflictException {

    public ReportAlreadyConfirmedException() {
        super("Report already confirmed by this user");
    }
}
