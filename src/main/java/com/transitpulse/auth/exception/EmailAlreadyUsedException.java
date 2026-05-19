package com.transitpulse.auth.exception;

import com.transitpulse.common.error.ConflictException;

public class EmailAlreadyUsedException extends ConflictException {

    public EmailAlreadyUsedException() {
        super("Email is already in use");
    }
}
