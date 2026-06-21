package com.marxAI.repository;

import com.marxAI.model.entity.Chunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Chunk} records produced during document ingestion. */
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    /** Returns a document's chunks in original order, e.g. for re-assembling source text. */
    List<Chunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    /** Removes all chunks for a document, used when a document is deleted or re-ingested. */
    void deleteByDocumentId(UUID documentId);
}
