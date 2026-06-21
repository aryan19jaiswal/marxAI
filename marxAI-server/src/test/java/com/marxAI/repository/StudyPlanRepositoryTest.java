package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.StudyPlan;
import com.marxAI.model.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link StudyPlanRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudyPlanRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsOnlyThatUsersPlansNewestFirst()
            throws InterruptedException {
        User owner = userRepository.save(
                User.builder().email("plan@example.com").name("Owner").passwordHash("hashed").build());
        User other = userRepository.save(
                User.builder().email("other-plan@example.com").name("Other").passwordHash("hashed").build());
        LocalDate start = LocalDate.of(2026, 6, 21);

        studyPlanRepository.save(StudyPlan.builder()
                .user(owner).startDate(start).endDate(start.plusDays(30)).planJson("{}").build());
        Thread.sleep(5);
        studyPlanRepository.save(StudyPlan.builder()
                .user(owner).startDate(start).endDate(start.plusDays(30)).planJson("{}").build());
        studyPlanRepository.save(StudyPlan.builder()
                .user(other).startDate(start).endDate(start.plusDays(30)).planJson("{}").build());

        List<StudyPlan> result = studyPlanRepository.findByUserIdOrderByCreatedAtDesc(owner.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getUser().getId().equals(owner.getId()));
        assertThat(result.get(0).getCreatedAt()).isAfterOrEqualTo(result.get(1).getCreatedAt());
    }
}
