package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Session;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link SessionRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void findByUserIdOrderByStartedAtDesc_returnsOnlyThatUsersSessionsNewestFirst() throws InterruptedException {
        User owner = userRepository.save(
                User.builder().email("owner@example.com").name("Owner").passwordHash("hashed").build());
        User other = userRepository.save(
                User.builder().email("other@example.com").name("Other").passwordHash("hashed").build());

        sessionRepository.save(Session.builder().user(owner).agentType("DSA").build());
        Thread.sleep(5);
        sessionRepository.save(Session.builder().user(owner).agentType("RESUME").build());
        sessionRepository.save(Session.builder().user(other).agentType("DSA").build());

        List<Session> result = sessionRepository.findByUserIdOrderByStartedAtDesc(owner.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.getUser().getId().equals(owner.getId()));
        assertThat(result.get(0).getStartedAt()).isAfterOrEqualTo(result.get(1).getStartedAt());
    }
}
