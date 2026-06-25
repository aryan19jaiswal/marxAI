package com.marxAI.controller;

import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.security.UserPrincipal;
import com.marxAI.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload endpoint for the {@code documents} resource. */
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;

    /**
     * Uploads a PDF/MD/TXT file to MinIO and records its metadata, owned by the calling user.
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
}
