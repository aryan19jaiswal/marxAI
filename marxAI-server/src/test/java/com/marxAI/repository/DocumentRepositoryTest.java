package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Document;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link DocumentRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void findByUserIdOrderByUploadedAtDesc_returnsOnlyThatUsersDocumentsNewestFirst()
            throws InterruptedException {
        User owner = userRepository.save(
                User.builder().email("docs@example.com").name("Owner").passwordHash("hashed").build());
        User other = userRepository.save(
                User.builder().email("other-docs@example.com").name("Other").passwordHash("hashed").build());

        documentRepository.save(Document.builder()
                .user(owner).filename("a.pdf").s3Key("a.pdf").docType("DSA").build());
        Thread.sleep(5);
        documentRepository.save(Document.builder()
                .user(owner).filename("b.pdf").s3Key("b.pdf").docType("RESUME").build());
        documentRepository.save(Document.builder()
                .user(other).filename("c.pdf").s3Key("c.pdf").docType("DSA").build());

        List<Document> result = documentRepository.findByUserIdOrderByUploadedAtDesc(owner.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getUser().getId().equals(owner.getId()));
        assertThat(result.get(0).getUploadedAt()).isAfterOrEqualTo(result.get(1).getUploadedAt());
    }
}
