package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Progress;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link ProgressRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProgressRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgressRepository progressRepository;

    @Test
    void findByUserId_returnsAllProgressRowsForThatUser() {
        User user = userRepository.save(
                User.builder().email("progress@example.com").name("Learner").passwordHash("hashed").build());
        progressRepository.save(Progress.builder().user(user).topic("DSA").subtopic("Arrays").build());
        progressRepository.save(Progress.builder().user(user).topic("DSA").subtopic("Trees").build());

        List<Progress> result = progressRepository.findByUserId(user.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserIdAndTopicAndSubtopic_returnsTheMatchingRow() {
        User user = userRepository.save(
                User.builder().email("progress2@example.com").name("Learner").passwordHash("hashed").build());
        progressRepository.save(Progress.builder()
                .user(user).topic("DSA").subtopic("Graphs").score(60).build());

        assertThat(progressRepository.findByUserIdAndTopicAndSubtopic(user.getId(), "DSA", "Graphs"))
                .isPresent()
                .get()
                .extracting(Progress::getScore)
                .isEqualTo(60);

        assertThat(progressRepository.findByUserIdAndTopicAndSubtopic(user.getId(), "DSA", "Strings"))
                .isEmpty();
    }
}
