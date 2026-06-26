package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.exception.DocumentNotFoundException;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.entity.Document;
import com.marxAI.model.entity.User;
import com.marxAI.repository.DocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link DocumentManagementService} (list and delete).
 * All collaborators are mocked so no infrastructure is required.
 */
@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private QdrantService qdrantService;

    @InjectMocks
    private DocumentManagementService service;

    // -------------------------------------------------------------------------
    // listByUser
    // -------------------------------------------------------------------------

    @Test
    void listByUser_noDocuments_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(userId)).thenReturn(List.of());

        List<DocumentResponse> result = service.listByUser(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void listByUser_withDocuments_returnsMappedResponses() {
        UUID userId = UUID.randomUUID();
        User owner = User.builder().id(userId).build();

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .user(owner)
                .filename("dsa-notes.pdf")
                .s3Key("key/dsa.pdf")
                .docType("DSA")
                .status("READY")
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findByUserIdOrderByUploadedAtDesc(userId)).thenReturn(List.of(doc));

        List<DocumentResponse> result = service.listByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).filename()).isEqualTo("dsa-notes.pdf");
        assertThat(result.get(0).docType()).isEqualTo("DSA");
        assertThat(result.get(0).status()).isEqualTo("READY");
    }

    // -------------------------------------------------------------------------
    // deleteDocument — happy path
    // -------------------------------------------------------------------------

    @Test
    void deleteDocument_ownDocument_deletesFromAllThreeStores() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        User owner = User.builder().id(userId).build();
        Document doc = Document.builder()
                .id(docId)
                .user(owner)
                .s3Key("user-id/doc.txt")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        service.deleteDocument(docId, userId);

        verify(qdrantService).deleteByDocumentId(docId);
        verify(storageService).deleteFile("user-id/doc.txt");
        verify(documentRepository).delete(doc);
    }

    // -------------------------------------------------------------------------
    // deleteDocument — error cases
    // -------------------------------------------------------------------------

    @Test
    void deleteDocument_notFound_throwsDocumentNotFoundException() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDocument(docId, UUID.randomUUID()))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(docId.toString());

        verify(documentRepository, never()).delete(any());
    }

    @Test
    void deleteDocument_differentOwner_throwsAccessDenied() {
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        Document doc = Document.builder().id(docId).user(owner).s3Key("key").build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.deleteDocument(docId, attackerId))
                .isInstanceOf(AccessDeniedException.class);

        // No cleanup should happen for an unauthorised attempt.
        verify(qdrantService, never()).deleteByDocumentId(any());
        verify(storageService, never()).deleteFile(any());
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void deleteDocument_minioFails_stillRemovesDbRecord() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        User owner = User.builder().id(userId).build();
        Document doc = Document.builder()
                .id(docId)
                .user(owner)
                .s3Key("user-id/doc.txt")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        doThrow(new RuntimeException("MinIO unavailable")).when(storageService).deleteFile(any());

        // Should NOT throw; MinIO failure is swallowed per best-effort policy.
        service.deleteDocument(docId, userId);

        verify(qdrantService).deleteByDocumentId(docId);
        verify(documentRepository).delete(doc);
    }
}
