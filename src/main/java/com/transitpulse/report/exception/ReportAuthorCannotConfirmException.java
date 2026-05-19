package com.transitpulse.report.exception;

import com.transitpulse.common.error.ForbiddenException;

public class ReportAuthorCannotConfirmException extends ForbiddenException {

    public ReportAuthorCannotConfirmException() {
        super("Report author cannot confirm own report");
    }
}
