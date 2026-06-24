package com.marxAI.model.dto;

/** Response for {@code register}/{@code login} carrying the issued JWT and the user profile. */
public record AuthResponse(String token, String tokenType, long expiresInMs, UserResponse user) {

    public static AuthResponse of(String token, long expiresInMs, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
