package com.marxAI.service;

import com.marxAI.model.chunking.ParsedDocument;
import com.marxAI.model.chunking.TextChunk;
import com.marxAI.model.entity.Chunk;
import com.marxAI.model.entity.Document;
import com.marxAI.repository.DocumentRepository;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs the background half of document ingestion: download the uploaded file from storage, parse
 * it, split it into chunks, embed and upsert those chunks into Qdrant, then flip the document to
 * {@code READY} (or {@code FAILED}). {@code IngestionService} triggers {@link #ingest(UUID)}
 * right after the upload response's metadata row is saved, so the upload request itself never
 * waits on parsing/embedding.
 */
@Service
@RequiredArgsConstructor
public class IngestionPipelineService {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipelineService.class);

    static final String STATUS_READY = "READY";
    static final String STATUS_FAILED = "FAILED";

    private final StorageService storageService;
    private final TikaDocumentParser tikaDocumentParser;
    private final ChunkingService chunkingService;
    private final QdrantService qdrantService;
    private final DocumentRepository documentRepository;

    /**
     * Parses, chunks, embeds, and upserts {@code documentId}'s file, then marks it {@code READY}
     * with its final {@code chunkCount}. On any failure the document is marked {@code FAILED} and
     * the exception is swallowed (logged only) — there is no caller waiting on this method's
     * result to report back to, since it runs on {@code ingestionExecutor} after the upload
     * response has already been sent.
     *
     * @param documentId id of a {@code documents} row already in {@code PROCESSING} status
     */
    @Async("ingestionExecutor")
    public void ingest(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("Skipping ingestion: document {} no longer exists", documentId);
            return;
        }

        try (InputStream content = storageService.downloadFile(document.getS3Key())) {
            ParsedDocument parsed = tikaDocumentParser.parse(content, document.getFilename());
            List<TextChunk> textChunks = chunkingService.chunk(parsed);
            List<Chunk> chunks = qdrantService.upsertChunks(document, textChunks);

            document.setStatus(STATUS_READY);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);
        } catch (Exception ex) {
            log.error("Ingestion failed for document {}", documentId, ex);
            document.setStatus(STATUS_FAILED);
            documentRepository.save(document);
        }
    }
}
