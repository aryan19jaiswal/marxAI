package com.marxAI.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.marxAI.model.dto.AuthResponse;
import com.marxAI.model.dto.DocumentResponse;
import com.marxAI.model.dto.ErrorResponse;
import com.marxAI.model.dto.RegisterRequest;
import com.marxAI.repository.DocumentRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * End-to-end tests for Day 13–14 document management endpoints:
 * {@code GET /api/docs} and {@code DELETE /api/docs/{id}}.
 *
 * <p>Requires Docker containers (PostgreSQL, MinIO, Qdrant) as configured by
 * {@code docker-compose.yml}. Runs against a random port to avoid conflicts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentManagementIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String registerAndGetToken() {
        String email = "doc-mgmt-" + UUID.randomUUID() + "@example.com";
        RegisterRequest req = new RegisterRequest("Doc Manager", email, "password123");
        AuthResponse auth = restTemplate.postForEntity("/api/users/register", req, AuthResponse.class)
                .getBody();
        return auth.token();
    }

    private static HttpEntity<MultiValueMap<String, Object>> uploadRequest(
            String token, String filename, byte[] content, String docType) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() { return filename; }
        });
        body.add("docType", docType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private DocumentResponse uploadDocument(String token, String docType) {
        byte[] content = ("sample content for " + docType).getBytes(StandardCharsets.UTF_8);
        return restTemplate.postForEntity(
                "/api/docs/upload",
                uploadRequest(token, "notes-" + UUID.randomUUID() + ".txt", content, docType),
                DocumentResponse.class
        ).getBody();
    }

    // -------------------------------------------------------------------------
    // GET /api/docs — list documents
    // -------------------------------------------------------------------------

    @Test
    void list_noDocuments_returnsEmptyArray() {
        String token = registerAndGetToken();

        ResponseEntity<List<DocumentResponse>> response = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void list_afterUpload_returnsDocumentInList() {
        String token = registerAndGetToken();
        uploadDocument(token, "DSA");

        ResponseEntity<List<DocumentResponse>> response = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<DocumentResponse> docs = response.getBody();
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).docType()).isEqualTo("DSA");
        assertThat(docs.get(0).status()).isEqualTo("PROCESSING");
    }

    @Test
    void list_multipleUploads_returnsNewestFirst() {
        String token = registerAndGetToken();
        uploadDocument(token, "DSA");
        uploadDocument(token, "SYSTEM_DESIGN");

        ResponseEntity<List<DocumentResponse>> response = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<DocumentResponse> docs = response.getBody();
        assertThat(docs).hasSize(2);
        // Newest upload (SYSTEM_DESIGN) should be first
        assertThat(docs.get(0).docType()).isEqualTo("SYSTEM_DESIGN");
    }

    @Test
    void list_withoutToken_returnsUnauthorized() {
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void list_isolatedPerUser_doesNotReturnOtherUsersDocuments() {
        String tokenA = registerAndGetToken();
        String tokenB = registerAndGetToken();
        uploadDocument(tokenA, "DSA");

        ResponseEntity<List<DocumentResponse>> responseB = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenB)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseB.getBody()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/docs/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_ownDocument_removesDbRowAndReturns204() {
        String token = registerAndGetToken();
        DocumentResponse uploaded = uploadDocument(token, "DSA");
        UUID docId = uploaded.id();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/docs/" + docId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(documentRepository.findById(docId)).isEmpty();
    }

    @Test
    void delete_ownDocument_disappearsFromList() {
        String token = registerAndGetToken();
        DocumentResponse uploaded = uploadDocument(token, "DSA");

        restTemplate.exchange(
                "/api/docs/" + uploaded.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                Void.class
        );

        ResponseEntity<List<DocumentResponse>> listResponse = restTemplate.exchange(
                "/api/docs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(listResponse.getBody()).isEmpty();
    }

    @Test
    void delete_nonExistentDocument_returns404() {
        String token = registerAndGetToken();

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/docs/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_anotherUsersDocument_returns403() {
        String tokenOwner = registerAndGetToken();
        String tokenAttacker = registerAndGetToken();
        DocumentResponse uploaded = uploadDocument(tokenOwner, "DSA");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/docs/" + uploaded.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(tokenAttacker)),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // Document must still exist after the unauthorized delete attempt.
        assertThat(documentRepository.findById(uploaded.id())).isPresent();
    }

    @Test
    void delete_withoutToken_returnsUnauthorized() {
        String token = registerAndGetToken();
        DocumentResponse uploaded = uploadDocument(token, "DSA");

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/docs/" + uploaded.id(),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
