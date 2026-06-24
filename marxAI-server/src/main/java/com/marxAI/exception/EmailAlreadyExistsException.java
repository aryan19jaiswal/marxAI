package com.marxAI.exception;

/** Thrown during registration when the requested email is already taken. */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
