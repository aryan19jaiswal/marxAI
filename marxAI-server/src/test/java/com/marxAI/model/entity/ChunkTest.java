package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Chunk} entity's builder, getters, and setters. */
class ChunkTest {

    @Test
    void builderPopulatesAllFields() {
        Document document = Document.builder().id(UUID.randomUUID()).build();

        Chunk chunk = Chunk.builder()
                .id(UUID.randomUUID())
                .document(document)
                .content("Binary search runs in O(log n) time.")
                .qdrantId("qdrant-vec-1")
                .chunkIndex(0)
                .metadata("{\"page\":1}")
                .build();

        assertThat(chunk.getDocument()).isEqualTo(document);
        assertThat(chunk.getContent()).isEqualTo("Binary search runs in O(log n) time.");
        assertThat(chunk.getQdrantId()).isEqualTo("qdrant-vec-1");
        assertThat(chunk.getChunkIndex()).isZero();
        assertThat(chunk.getMetadata()).isEqualTo("{\"page\":1}");
    }

    @Test
    void settersMutateFields() {
        Chunk chunk = new Chunk();
        chunk.setChunkIndex(3);

        assertThat(chunk.getChunkIndex()).isEqualTo(3);
    }
}
