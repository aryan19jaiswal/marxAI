package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Session} entity's builder, getters, and setters. */
class SessionTest {

    @Test
    void builderPopulatesAllFields() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Instant started = Instant.now();

        Session session = Session.builder()
                .id(UUID.randomUUID())
                .user(user)
                .agentType("DSA")
                .metadata("{\"topic\":\"trees\"}")
                .startedAt(started)
                .endedAt(null)
                .build();

        assertThat(session.getUser()).isEqualTo(user);
        assertThat(session.getAgentType()).isEqualTo("DSA");
        assertThat(session.getMetadata()).isEqualTo("{\"topic\":\"trees\"}");
        assertThat(session.getStartedAt()).isEqualTo(started);
        assertThat(session.getEndedAt()).isNull();
    }

    @Test
    void settersMutateFields() {
        Session session = new Session();
        Instant endedAt = Instant.now();
        session.setEndedAt(endedAt);

        assertThat(session.getEndedAt()).isEqualTo(endedAt);
    }
}
