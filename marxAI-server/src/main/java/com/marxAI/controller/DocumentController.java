package com.marxAI.controller;

import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.security.UserPrincipal;
import com.marxAI.service.DocumentManagementService;
import com.marxAI.service.IngestionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST resource for the {@code /api/docs} document management endpoints.
 *
 * <p>Day 8: {@code POST /upload} — ingest a new file into the RAG pipeline.
 * <p>Day 13–14: {@code GET /} — list the caller's documents; {@code DELETE /{id}} — remove one.
 */
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentManagementService documentManagementService;

    /**
     * Uploads a PDF/MD/TXT file to MinIO and records its metadata, owned by the calling user.
     * Ingestion (parse → chunk → embed) runs asynchronously; the document is returned in
     * {@code PROCESSING} status immediately.
     *
     * @param file the uploaded file (multipart form field {@code file})
     * @param docType knowledge-base category, e.g. {@code DSA}, {@code SYSTEM_DESIGN}, {@code RESUME}
     * @param principal the authenticated caller, resolved from the JWT
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docType,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse response = ingestionService.uploadDocument(file, docType, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all documents belonging to the authenticated user, newest-first.
     * Used by the documents management UI to render the document list table.
     *
     * @param principal the authenticated caller
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentManagementService.listByUser(principal.getId()));
    }

    /**
     * Deletes the document identified by {@code id}, including its Qdrant vectors, Postgres
     * chunk rows, and MinIO object. Returns {@code 204 No Content} on success.
     *
     * @param id the document to delete
     * @param principal the authenticated caller; must be the document's owner
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentManagementService.deleteDocument(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
