package com.marxAI.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Payload for {@code POST /api/chat} and {@code POST /api/chat/stream}.
 *
 * @param sessionId optional existing session ID to resume; omit (null) to start a new session
 * @param message   the user's chat message — must not be blank
 */
public record ChatRequest(UUID sessionId, @NotBlank String message) {}
