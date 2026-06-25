package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.exception.DocumentParsingException;
import com.marxAI.model.chunking.ParsedDocument;
import com.marxAI.model.chunking.TextChunk;
import com.marxAI.model.entity.Chunk;
import com.marxAI.model.entity.Document;
import com.marxAI.repository.DocumentRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link IngestionPipelineService}, with all collaborators mocked. */
@ExtendWith(MockitoExtension.class)
class IngestionPipelineServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private TikaDocumentParser tikaDocumentParser;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private QdrantService qdrantService;

    @Mock
    private DocumentRepository documentRepository;

    private IngestionPipelineService ingestionPipelineService;
    private Document document;

    @BeforeEach
    void setUp() {
        ingestionPipelineService = new IngestionPipelineService(
                storageService, tikaDocumentParser, chunkingService, qdrantService, documentRepository);
        document = Document.builder()
                .id(UUID.randomUUID())
                .filename("notes.pdf")
                .s3Key("user-1/notes.pdf")
                .docType("DSA")
                .status("PROCESSING")
                .build();
    }

    private static InputStream emptyStream() {
        return new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void ingest_doesNothing_whenDocumentNoLongerExists() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.empty());

        ingestionPipelineService.ingest(document.getId());

        verify(storageService, never()).downloadFile(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void ingest_parsesChunksAndUpserts_thenMarksDocumentReadyWithChunkCount() {
        ParsedDocument parsed = new ParsedDocument("full text", List.of("full text"));
        List<TextChunk> textChunks = List.of(new TextChunk(0, 1, "full text"));
        List<Chunk> persistedChunks =
                List.of(Chunk.builder().document(document).content("full text").qdrantId("q-1").chunkIndex(0).build());

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(storageService.downloadFile(document.getS3Key())).thenReturn(emptyStream());
        when(tikaDocumentParser.parse(any(), eq(document.getFilename()))).thenReturn(parsed);
        when(chunkingService.chunk(parsed)).thenReturn(textChunks);
        when(qdrantService.upsertChunks(document, textChunks)).thenReturn(persistedChunks);

        ingestionPipelineService.ingest(document.getId());

        assertThat(document.getStatus()).isEqualTo("READY");
        assertThat(document.getChunkCount()).isEqualTo(1);
        verify(documentRepository).save(document);
    }

    @Test
    void ingest_marksDocumentFailed_whenParsingThrows() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(storageService.downloadFile(document.getS3Key())).thenReturn(emptyStream());
        when(tikaDocumentParser.parse(any(), eq(document.getFilename())))
                .thenThrow(new DocumentParsingException(document.getFilename(), new RuntimeException("corrupt")));

        ingestionPipelineService.ingest(document.getId());

        assertThat(document.getStatus()).isEqualTo("FAILED");
        verify(documentRepository).save(document);
        verify(qdrantService, never()).upsertChunks(any(), any());
    }

    @Test
    void ingest_marksDocumentFailed_whenUpsertThrows() {
        ParsedDocument parsed = new ParsedDocument("full text", List.of("full text"));
        List<TextChunk> textChunks = List.of(new TextChunk(0, 1, "full text"));

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(storageService.downloadFile(document.getS3Key())).thenReturn(emptyStream());
        when(tikaDocumentParser.parse(any(), eq(document.getFilename()))).thenReturn(parsed);
        when(chunkingService.chunk(parsed)).thenReturn(textChunks);
        when(qdrantService.upsertChunks(document, textChunks)).thenThrow(new RuntimeException("qdrant unreachable"));

        ingestionPipelineService.ingest(document.getId());

        assertThat(document.getStatus()).isEqualTo("FAILED");
        verify(documentRepository).save(document);
    }
}
