package com.transitpulse.auth.exception;

import com.transitpulse.common.error.UnauthorizedException;

public class CurrentUserNotFoundException extends UnauthorizedException {

    public CurrentUserNotFoundException() {
        super("Current user not found");
    }
}
