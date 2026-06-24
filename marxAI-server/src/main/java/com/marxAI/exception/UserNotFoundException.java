package com.marxAI.exception;

import java.util.UUID;

/** Thrown when a user referenced by id (e.g. JWT subject) no longer exists. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("No user found with id '" + userId + "'");
    }
}
