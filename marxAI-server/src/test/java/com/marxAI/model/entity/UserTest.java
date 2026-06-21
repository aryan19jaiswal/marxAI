package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link User} entity's builder, getters, and setters. */
class UserTest {

    @Test
    void builderPopulatesAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user = User.builder()
                .id(id)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed-password")
                .preferences("{\"theme\":\"dark\"}")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getName()).isEqualTo("Test User");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getPreferences()).isEqualTo("{\"theme\":\"dark\"}");
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void settersMutateFields() {
        User user = new User();
        user.setEmail("changed@example.com");
        user.setName("Changed Name");

        assertThat(user.getEmail()).isEqualTo("changed@example.com");
        assertThat(user.getName()).isEqualTo("Changed Name");
    }
}
