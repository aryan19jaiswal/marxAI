package com.marxAI.model.dto;

import com.marxAI.model.entity.User;
import java.time.Instant;
import java.util.UUID;

/** Public-facing view of a {@link User}, never exposes the password hash. */
public record UserResponse(UUID id, String email, String name, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
