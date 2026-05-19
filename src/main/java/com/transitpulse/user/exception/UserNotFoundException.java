package com.transitpulse.user.exception;

import com.transitpulse.common.error.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("User not found");
    }
}
