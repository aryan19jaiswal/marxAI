package com.marxAI.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.entity.Chunk;
import com.marxAI.model.entity.Document;
import com.marxAI.model.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Repository-layer tests for {@link ChunkRepository}, run against the real Flyway-migrated
 * Postgres schema (no embedded DB on the classpath). Each test rolls back automatically.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChunkRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ChunkRepository chunkRepository;

    @Test
    void findByDocumentIdOrderByChunkIndexAsc_returnsChunksInIndexOrder() {
        Document document = createDocument();

        chunkRepository.save(Chunk.builder()
                .document(document).content("second").qdrantId("q2").chunkIndex(2).build());
        chunkRepository.save(Chunk.builder()
                .document(document).content("first").qdrantId("q0").chunkIndex(0).build());

        List<Chunk> result = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId());

        assertThat(result).extracting(Chunk::getChunkIndex).containsExactly(0, 2);
    }

    @Test
    void deleteByDocumentId_removesAllChunksForThatDocument() {
        Document document = createDocument();
        chunkRepository.save(Chunk.builder()
                .document(document).content("c1").qdrantId("q1").chunkIndex(0).build());
        chunkRepository.save(Chunk.builder()
                .document(document).content("c2").qdrantId("q2").chunkIndex(1).build());

        chunkRepository.deleteByDocumentId(document.getId());

        assertThat(chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId())).isEmpty();
    }

    private Document createDocument() {
        User user = userRepository.save(
                User.builder().email("chunks@example.com").name("Owner").passwordHash("hashed").build());
        return documentRepository.save(Document.builder()
                .user(user).filename("notes.pdf").s3Key("notes.pdf").docType("DSA").build());
    }
}
