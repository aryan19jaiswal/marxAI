package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Document} entity's builder, getters, defaults, and setters. */
class DocumentTest {

    @Test
    void builderPopulatesAllFields() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Instant now = Instant.now();

        Document document = Document.builder()
                .id(UUID.randomUUID())
                .user(user)
                .filename("dsa-notes.pdf")
                .s3Key("docs/dsa-notes.pdf")
                .docType("DSA")
                .status("READY")
                .chunkCount(12)
                .metadata("{\"pages\":10}")
                .uploadedAt(now)
                .build();

        assertThat(document.getUser()).isEqualTo(user);
        assertThat(document.getFilename()).isEqualTo("dsa-notes.pdf");
        assertThat(document.getS3Key()).isEqualTo("docs/dsa-notes.pdf");
        assertThat(document.getDocType()).isEqualTo("DSA");
        assertThat(document.getStatus()).isEqualTo("READY");
        assertThat(document.getChunkCount()).isEqualTo(12);
        assertThat(document.getMetadata()).isEqualTo("{\"pages\":10}");
        assertThat(document.getUploadedAt()).isEqualTo(now);
    }

    @Test
    void builderDefaultsStatusAndChunkCountWhenUnset() {
        Document document = Document.builder().build();

        assertThat(document.getStatus()).isEqualTo("PROCESSING");
        assertThat(document.getChunkCount()).isZero();
    }

    @Test
    void settersMutateFields() {
        Document document = new Document();
        document.setStatus("FAILED");

        assertThat(document.getStatus()).isEqualTo("FAILED");
    }
}
