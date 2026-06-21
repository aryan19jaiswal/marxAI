package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Resume;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link ResumeRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResumeRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Test
    void findByUserIdOrderByUploadedAtDesc_returnsOnlyThatUsersResumesNewestFirst()
            throws InterruptedException {
        User owner = userRepository.save(
                User.builder().email("resume@example.com").name("Owner").passwordHash("hashed").build());
        User other = userRepository.save(
                User.builder().email("other-resume@example.com").name("Other").passwordHash("hashed").build());

        resumeRepository.save(Resume.builder().user(owner).s3Key("v1.pdf").build());
        Thread.sleep(5);
        resumeRepository.save(Resume.builder().user(owner).s3Key("v2.pdf").build());
        resumeRepository.save(Resume.builder().user(other).s3Key("v1.pdf").build());

        List<Resume> result = resumeRepository.findByUserIdOrderByUploadedAtDesc(owner.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getUser().getId().equals(owner.getId()));
        assertThat(result.get(0).getUploadedAt()).isAfterOrEqualTo(result.get(1).getUploadedAt());
    }
}
