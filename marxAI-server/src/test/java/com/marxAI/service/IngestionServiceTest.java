package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.exception.InvalidDocumentTypeException;
import com.marxAI.exception.UnsupportedFileTypeException;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.entity.Document;
import com.marxAI.model.entity.User;
import com.marxAI.repository.DocumentRepository;
import com.marxAI.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/** Unit tests for {@link IngestionService}, with {@link StorageService} and repositories mocked. */
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IngestionPipelineService ingestionPipelineService;

    private IngestionService ingestionService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        ingestionService =
                new IngestionService(storageService, documentRepository, userRepository, ingestionPipelineService);
        userId = UUID.randomUUID();
        // Not every test reaches the storage/persistence step (some fail validation first).
        lenient().when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    }

    private static MultipartFile pdfFile() {
        return new MockMultipartFile("file", "notes.pdf", "application/pdf", "pdf bytes".getBytes());
    }

    @Test
    void uploadDocument_uploadsToStorageAndSavesProcessingDocument_whenValid() {
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        DocumentResponse response = ingestionService.uploadDocument(pdfFile(), "dsa", userId);

        ArgumentCaptor<Document> savedDoc = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(savedDoc.capture());
        assertThat(savedDoc.getValue().getFilename()).isEqualTo("notes.pdf");
        assertThat(savedDoc.getValue().getDocType()).isEqualTo("DSA");
        assertThat(savedDoc.getValue().getStatus()).isEqualTo("PROCESSING");
        assertThat(savedDoc.getValue().getS3Key()).startsWith(userId + "/").endsWith(".pdf");

        verify(storageService).uploadFile(any(MultipartFile.class), eq(savedDoc.getValue().getS3Key()));

        assertThat(response.filename()).isEqualTo("notes.pdf");
        assertThat(response.docType()).isEqualTo("DSA");
        assertThat(response.status()).isEqualTo("PROCESSING");

        verify(ingestionPipelineService).ingest(savedDoc.getValue().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"notes.pdf", "notes.md", "notes.txt", "NOTES.PDF"})
    void uploadDocument_acceptsAllowedExtensions(String filename) {
        MultipartFile file = new MockMultipartFile("file", filename, "text/plain", "content".getBytes());
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = ingestionService.uploadDocument(file, "RESUME", userId);

        assertThat(response.filename()).isEqualTo(filename);
        verify(storageService).uploadFile(eq(file), any());
    }

    @Test
    void uploadDocument_throwsInvalidDocumentType_andNeverTouchesStorage_whenDocTypeUnknown() {
        assertThatThrownBy(() -> ingestionService.uploadDocument(pdfFile(), "NOT_A_TYPE", userId))
                .isInstanceOf(InvalidDocumentTypeException.class)
                .hasMessageContaining("NOT_A_TYPE");

        verify(storageService, never()).uploadFile(any(), any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_throwsUnsupportedFileType_andNeverTouchesStorage_whenExtensionNotAllowed() {
        MultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> ingestionService.uploadDocument(exeFile, "DSA", userId))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("malware.exe");

        verify(storageService, never()).uploadFile(any(), any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_deletesUploadedObject_whenMetadataSaveFails() {
        RuntimeException dbFailure = new RuntimeException("db is down");
        when(documentRepository.save(any(Document.class))).thenThrow(dbFailure);

        assertThatThrownBy(() -> ingestionService.uploadDocument(pdfFile(), "DSA", userId)).isSameAs(dbFailure);

        ArgumentCaptor<String> deletedKey = ArgumentCaptor.forClass(String.class);
        verify(storageService).uploadFile(any(MultipartFile.class), deletedKey.capture());
        verify(storageService).deleteFile(deletedKey.getValue());
        verify(ingestionPipelineService, never()).ingest(any());
    }
}
