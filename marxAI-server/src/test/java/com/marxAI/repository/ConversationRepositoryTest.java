package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Conversation;
import com.marxAI.model.entity.Session;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link ConversationRepository}, run against the real
 * Flyway-migrated Postgres schema (no embedded DB on the classpath). Each test rolls back
 * automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConversationRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    void findBySessionIdOrderByCreatedAtAsc_returnsOnlyThatSessionsMessagesOldestFirst()
            throws InterruptedException {
        User user = userRepository.save(
                User.builder().email("chat@example.com").name("Chatter").passwordHash("hashed").build());
        Session session = sessionRepository.save(Session.builder().user(user).agentType("DSA").build());
        Session otherSession = sessionRepository.save(Session.builder().user(user).agentType("DSA").build());

        conversationRepository.save(
                Conversation.builder().session(session).role("user").content("Explain DP").build());
        Thread.sleep(5);
        conversationRepository.save(
                Conversation.builder().session(session).role("assistant").content("Sure, DP is...").build());
        conversationRepository.save(
                Conversation.builder().session(otherSession).role("user").content("Unrelated").build());

        List<Conversation> result = conversationRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getSession().getId().equals(session.getId()));
        assertThat(result.get(0).getCreatedAt()).isBeforeOrEqualTo(result.get(1).getCreatedAt());
    }
}
