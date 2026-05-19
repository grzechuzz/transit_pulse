package com.transitpulse.report.exception;

import com.transitpulse.common.error.ConflictException;

public class ReportNotPendingException extends ConflictException {

    public ReportNotPendingException(String action) {
        super("Only pending reports can be " + action);
    }
}
