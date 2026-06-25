package com.marxAI.model.dto;

import com.marxAI.model.entity.Document;
import java.time.Instant;
import java.util.UUID;

/** Public-facing view of a {@link Document}, returned after a successful upload. */
public record DocumentResponse(UUID id, String filename, String docType, String status, Instant uploadedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getDocType(),
                document.getStatus(),
                document.getUploadedAt());
    }
}
