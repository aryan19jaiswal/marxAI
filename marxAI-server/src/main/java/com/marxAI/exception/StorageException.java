package com.marxAI.exception;

/**
 * Thrown when a MinIO/S3 operation fails. Wraps the storage SDK's checked exceptions (network
 * errors, malformed responses, etc.) so callers only need to handle one unchecked type.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
