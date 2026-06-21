package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Progress} entity's builder, defaults, and setters. */
class ProgressTest {

    @Test
    void builderPopulatesAllFields() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Instant lastPracticed = Instant.now();

        Progress progress = Progress.builder()
                .id(UUID.randomUUID())
                .user(user)
                .topic("DSA")
                .subtopic("Dynamic Programming")
                .score(80)
                .status("IN_PROGRESS")
                .attempts(3)
                .lastPracticed(lastPracticed)
                .build();

        assertThat(progress.getUser()).isEqualTo(user);
        assertThat(progress.getTopic()).isEqualTo("DSA");
        assertThat(progress.getSubtopic()).isEqualTo("Dynamic Programming");
        assertThat(progress.getScore()).isEqualTo(80);
        assertThat(progress.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(progress.getAttempts()).isEqualTo(3);
        assertThat(progress.getLastPracticed()).isEqualTo(lastPracticed);
    }

    @Test
    void builderDefaultsScoreStatusAndAttemptsWhenUnset() {
        Progress progress = Progress.builder().build();

        assertThat(progress.getScore()).isZero();
        assertThat(progress.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(progress.getAttempts()).isZero();
    }

    @Test
    void settersMutateFields() {
        Progress progress = new Progress();
        progress.setAttempts(5);

        assertThat(progress.getAttempts()).isEqualTo(5);
    }
}
