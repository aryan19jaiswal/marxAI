package com.marxAI.exception;

import java.util.UUID;

/**
 * Thrown when a {@code sessionId} is supplied in a chat request but the corresponding
 * {@link com.marxAI.model.entity.Session} cannot be found in the database.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Session not found: " + sessionId);
    }
}
