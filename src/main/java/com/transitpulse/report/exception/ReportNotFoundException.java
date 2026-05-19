package com.transitpulse.report.exception;

import com.transitpulse.common.error.NotFoundException;

public class ReportNotFoundException extends NotFoundException {

    public ReportNotFoundException() {
        super("Report not found");
    }
}
