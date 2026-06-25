package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marxAI.config.MinioConfig;
import com.marxAI.exception.StorageException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Integration tests for {@link StorageService}, run against the real local MinIO container (no
 * mocking of the SDK), mirroring how {@code DocumentRepositoryTest} exercises the real Postgres
 * instance. Requires {@code docker compose up} to be running.
 */
@SpringBootTest(classes = {MinioConfig.class, StorageService.class})
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    private static String uniqueKey(String suffix) {
        return "test/" + UUID.randomUUID() + "-" + suffix;
    }

    @Test
    void uploadFile_thenDownloadFile_roundTripsContent() throws Exception {
        String key = uniqueKey("roundtrip.txt");
        byte[] content = "hello marxai".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "roundtrip.txt", "text/plain", content);

        storageService.uploadFile(file, key);

        try (InputStream downloaded = storageService.downloadFile(key)) {
            assertThat(downloaded.readAllBytes()).isEqualTo(content);
        } finally {
            storageService.deleteFile(key);
        }
    }

    @Test
    void getPresignedUrl_returnsUrlThatServesTheUploadedContent() throws Exception {
        String key = uniqueKey("presigned.txt");
        byte[] content = "presigned content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "presigned.txt", "text/plain", content);
        storageService.uploadFile(file, key);

        try {
            String url = storageService.getPresignedUrl(key);
            assertThat(url).startsWith("http").contains(key);

            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create(url)).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(content);
        } finally {
            storageService.deleteFile(key);
        }
    }

    @Test
    void downloadFile_throwsStorageException_whenObjectDoesNotExist() {
        String key = uniqueKey("missing.txt");

        assertThatThrownBy(() -> storageService.downloadFile(key)).isInstanceOf(StorageException.class);
    }

    @Test
    void deleteFile_removesObject_soSubsequentDownloadFails() {
        String key = uniqueKey("to-delete.txt");
        byte[] content = "will be deleted".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "to-delete.txt", "text/plain", content);
        storageService.uploadFile(file, key);

        storageService.deleteFile(key);

        assertThatThrownBy(() -> storageService.downloadFile(key)).isInstanceOf(StorageException.class);
    }
}
