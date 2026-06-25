package com.marxAI.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.dto.AuthResponse;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.dto.ErrorResponse;
import com.marxAI.model.dto.RegisterRequest;
import com.marxAI.model.entity.Document;
import com.marxAI.repository.DocumentRepository;
import com.marxAI.service.StorageService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * End-to-end exercise of {@code POST /api/docs/upload}: register/login to obtain a JWT, then
 * upload a file through the real MinIO container and verify both the PostgreSQL record and the
 * stored object.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentUploadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StorageService storageService;

    private String registerAndGetToken() {
        String email = "doc-upload-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Doc Uploader", email, "password123");
        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/users/register", registerRequest, AuthResponse.class);
        return response.getBody().token();
    }

    private static HttpEntity<MultiValueMap<String, Object>> multipartRequest(
            String token, String filename, byte[] content, String docType) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("docType", docType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    @Test
    void upload_validTextFile_persistsMetadataAndStoresObjectInMinio() throws Exception {
        String token = registerAndGetToken();
        byte[] content = "binary search is O(log n)".getBytes(StandardCharsets.UTF_8);

        ResponseEntity<DocumentResponse> response = restTemplate.postForEntity(
                "/api/docs/upload",
                multipartRequest(token, "dsa-notes.txt", content, "DSA"),
                DocumentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.filename()).isEqualTo("dsa-notes.txt");
        assertThat(body.docType()).isEqualTo("DSA");
        assertThat(body.status()).isEqualTo("PROCESSING");

        Document persisted = documentRepository.findById(body.id()).orElseThrow();
        assertThat(persisted.getFilename()).isEqualTo("dsa-notes.txt");

        try (InputStream stored = storageService.downloadFile(persisted.getS3Key())) {
            assertThat(stored.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void upload_invalidDocType_returnsBadRequest() {
        String token = registerAndGetToken();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/docs/upload",
                multipartRequest(token, "notes.txt", "content".getBytes(StandardCharsets.UTF_8), "NOT_A_REAL_TYPE"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("NOT_A_REAL_TYPE");
    }

    @Test
    void upload_unsupportedFileExtension_returnsBadRequest() {
        String token = registerAndGetToken();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/docs/upload",
                multipartRequest(token, "malware.exe", "content".getBytes(StandardCharsets.UTF_8), "DSA"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("malware.exe");
    }

    @Test
    void upload_withoutToken_returnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/docs/upload",
                multipartRequest(null, "notes.txt", "content".getBytes(StandardCharsets.UTF_8), "DSA"),
                ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
