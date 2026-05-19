package com.transitpulse.auth.exception;

import com.transitpulse.common.error.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
