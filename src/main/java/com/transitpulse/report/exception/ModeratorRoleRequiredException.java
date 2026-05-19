package com.transitpulse.report.exception;

import com.transitpulse.common.error.ForbiddenException;

public class ModeratorRoleRequiredException extends ForbiddenException {

    public ModeratorRoleRequiredException() {
        super("Moderator or admin role is required");
    }
}
