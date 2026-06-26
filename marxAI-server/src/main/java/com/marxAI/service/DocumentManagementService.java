package com.marxAI.service;

import com.marxAI.exception.DocumentNotFoundException;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.entity.Document;
import com.marxAI.repository.DocumentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day 13–14: document management operations exposed by the UI — list and delete.
 *
 * <p>Deletion is a three-store operation: Qdrant vectors + Postgres chunk rows (via
 * {@link QdrantService#deleteByDocumentId}), the MinIO object, and the {@code documents} row
 * itself. The Postgres operations run inside a transaction so they roll back together on failure.
 * Qdrant/MinIO are external and not transactional, but that's acceptable: if they succeed and the
 * DB write rolls back, re-running the delete will simply no-op on the already-cleaned external
 * stores.
 */
@Service
@RequiredArgsConstructor
public class DocumentManagementService {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final QdrantService qdrantService;

    /**
     * Returns all documents belonging to {@code userId}, newest-first, as lightweight DTOs.
     *
     * @param userId the authenticated user's id
     * @return list of documents; empty if the user has not uploaded anything yet
     */
    public List<DocumentResponse> listByUser(UUID userId) {
        return documentRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Deletes the document identified by {@code documentId} if it is owned by {@code userId}.
     *
     * <p>Deletion order:
     * <ol>
     *   <li>Verify the document exists and belongs to the requesting user.
     *   <li>Remove Qdrant vectors and Postgres chunk rows ({@link QdrantService#deleteByDocumentId}).
     *   <li>Remove the MinIO object.
     *   <li>Remove the {@code documents} row.
     * </ol>
     *
     * @param documentId the document to remove
     * @param userId     the authenticated caller; must match the document's owner
     * @throws DocumentNotFoundException if {@code documentId} resolves to no record
     * @throws AccessDeniedException     if the caller does not own the document
     */
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete document " + documentId);
        }

        // Clean up Qdrant vectors and Postgres chunk rows first so the document row can be
        // deleted without violating the FK constraint on chunks.document_id.
        qdrantService.deleteByDocumentId(documentId);

        // MinIO object removal is best-effort; log and continue on failure so the DB record
        // is always cleaned up even when storage is temporarily unreachable.
        try {
            storageService.deleteFile(document.getS3Key());
        } catch (Exception ex) {
            log.warn("Failed to delete MinIO object '{}' for document {}; DB record will still be removed",
                    document.getS3Key(), documentId, ex);
        }

        documentRepository.delete(document);
        log.info("Deleted document {} (s3Key='{}') for user {}", documentId, document.getS3Key(), userId);
    }
}
