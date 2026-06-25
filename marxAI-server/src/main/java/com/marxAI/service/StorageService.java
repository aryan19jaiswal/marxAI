package com.marxAI.service;

import com.marxAI.config.MinioProperties;
import com.marxAI.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Thin wrapper around the MinIO SDK for the single bucket ({@link MinioProperties#bucket()}) that
 * holds all user-uploaded documents. Every object key is expected to already be unique (see
 * {@link IngestionService}); this class is not responsible for naming.
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /** Creates the configured bucket on startup if it doesn't already exist. */
    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created MinIO bucket '{}'", properties.bucket());
            }
        } catch (Exception ex) {
            throw new StorageException("Failed to verify/create MinIO bucket '" + properties.bucket() + "'", ex);
        }
    }

    /**
     * Streams {@code file} into the bucket under {@code objectKey}.
     *
     * @param file the multipart file received by the controller
     * @param objectKey destination key; callers are responsible for uniqueness
     */
    public void uploadFile(MultipartFile file, String objectKey) {
        try (InputStream content = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(content, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (IOException ex) {
            throw new StorageException("Failed to read upload stream for object '" + objectKey + "'", ex);
        } catch (Exception ex) {
            throw new StorageException("Failed to upload object '" + objectKey + "' to MinIO", ex);
        }
    }

    /**
     * Opens a stream to the object's contents. The caller owns the returned stream and must close
     * it (e.g. in a try-with-resources block).
     */
    public InputStream downloadFile(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception ex) {
            throw new StorageException("Failed to download object '" + objectKey + "' from MinIO", ex);
        }
    }

    /** Returns a short-lived, pre-signed GET URL for the object, valid for {@value #PRESIGNED_URL_EXPIRY_MINUTES} minutes. */
    public String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                    .build());
        } catch (Exception ex) {
            throw new StorageException("Failed to presign URL for object '" + objectKey + "'", ex);
        }
    }

    /** Deletes the object. Used to roll back an upload when the follow-up metadata write fails. */
    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception ex) {
            throw new StorageException("Failed to delete object '" + objectKey + "' from MinIO", ex);
        }
    }
}
