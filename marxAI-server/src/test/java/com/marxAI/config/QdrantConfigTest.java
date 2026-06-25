package com.marxAI.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for {@link QdrantConfig}, run against the real local Qdrant container (no
 * mocking of the SDK), mirroring how {@code StorageServiceTest} exercises the real MinIO
 * instance. Requires {@code docker compose up} to be running.
 */
@SpringBootTest(classes = {QdrantConfig.class})
class QdrantConfigTest {

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private QdrantProperties qdrantProperties;

    @Test
    void contextStartup_createsConfiguredCollection_whenItDoesNotAlreadyExist() throws Exception {
        assertThat(embeddingStore).isNotNull();

        boolean exists = qdrantClient.collectionExistsAsync(qdrantProperties.collectionName()).get();

        assertThat(exists).isTrue();
    }
}
