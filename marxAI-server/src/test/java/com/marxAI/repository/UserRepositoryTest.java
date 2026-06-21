package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link UserRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUser_whenEmailExists() {
        userRepository.save(User.builder()
                .email("jane@example.com")
                .name("Jane")
                .passwordHash("hashed")
                .build());

        assertThat(userRepository.findByEmail("jane@example.com"))
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo("Jane");
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPersistedState() {
        assertThat(userRepository.existsByEmail("new@example.com")).isFalse();

        userRepository.save(User.builder()
                .email("new@example.com")
                .name("New User")
                .passwordHash("hashed")
                .build());

        assertThat(userRepository.existsByEmail("new@example.com")).isTrue();
    }
}
