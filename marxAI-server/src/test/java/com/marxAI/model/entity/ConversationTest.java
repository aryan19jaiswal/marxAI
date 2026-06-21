package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Conversation} entity's builder, getters, and setters. */
class ConversationTest {

    @Test
    void builderPopulatesAllFields() {
        Session session = Session.builder().id(UUID.randomUUID()).build();
        Instant now = Instant.now();

        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .session(session)
                .role("assistant")
                .content("Here is an explanation of binary search.")
                .toolCalls("[{\"tool\":\"searchNotes\"}]")
                .tokensUsed(128)
                .createdAt(now)
                .build();

        assertThat(conversation.getSession()).isEqualTo(session);
        assertThat(conversation.getRole()).isEqualTo("assistant");
        assertThat(conversation.getContent()).isEqualTo("Here is an explanation of binary search.");
        assertThat(conversation.getToolCalls()).isEqualTo("[{\"tool\":\"searchNotes\"}]");
        assertThat(conversation.getTokensUsed()).isEqualTo(128);
        assertThat(conversation.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void settersMutateFields() {
        Conversation conversation = new Conversation();
        conversation.setRole("user");
        conversation.setContent("What is dynamic programming?");

        assertThat(conversation.getRole()).isEqualTo("user");
        assertThat(conversation.getContent()).isEqualTo("What is dynamic programming?");
    }
}
