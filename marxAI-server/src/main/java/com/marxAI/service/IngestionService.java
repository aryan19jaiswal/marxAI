package com.marxAI.service;

import com.marxAI.exception.InvalidDocumentTypeException;
import com.marxAI.exception.UnsupportedFileTypeException;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.entity.Document;
import com.marxAI.model.enums.DocumentType;
import com.marxAI.repository.DocumentRepository;
import com.marxAI.repository.UserRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates and persists a document upload: checks the file extension and {@code docType}, hands
 * the bytes to {@link StorageService}, then records the metadata row in PostgreSQL. Once the
 * metadata row is saved, parsing/chunking/embedding is handed off to {@link
 * IngestionPipelineService} to run in the background, so a freshly uploaded document is returned
 * in {@code PROCESSING} status without the caller waiting on it.
 */
@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "md", "txt");

    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final IngestionPipelineService ingestionPipelineService;

    /**
     * Uploads {@code file} to MinIO, records it for {@code userId}, then triggers background
     * ingestion ({@link IngestionPipelineService#ingest(UUID)}).
     *
     * @param file the multipart file from the request
     * @param docType raw {@code docType} request parameter, validated against {@link DocumentType}
     * @param userId owner of the upload, taken from the authenticated principal
     * @return metadata for the newly created document, in {@code PROCESSING} status
     * @throws InvalidDocumentTypeException if {@code docType} doesn't match a {@link DocumentType}
     * @throws UnsupportedFileTypeException if the file's extension isn't pdf/md/txt
     */
    public DocumentResponse uploadDocument(MultipartFile file, String docType, UUID userId) {
        DocumentType type = parseDocumentType(docType);
        String filename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String extension = extractExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedFileTypeException(filename, ALLOWED_EXTENSIONS);
        }

        String objectKey = userId + "/" + UUID.randomUUID() + "." + extension;
        storageService.uploadFile(file, objectKey);

        Document document = Document.builder()
                .user(userRepository.getReferenceById(userId))
                .filename(filename)
                .s3Key(objectKey)
                .docType(type.name())
                .build();

        Document saved;
        try {
            saved = documentRepository.save(document);
        } catch (RuntimeException ex) {
            // Don't leave an orphaned blob in MinIO if the metadata write fails.
            storageService.deleteFile(objectKey);
            throw ex;
        }

        ingestionPipelineService.ingest(saved.getId());
        return DocumentResponse.from(saved);
    }

    private static DocumentType parseDocumentType(String docType) {
        try {
            return DocumentType.valueOf(docType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidDocumentTypeException(docType);
        }
    }

    private static String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
