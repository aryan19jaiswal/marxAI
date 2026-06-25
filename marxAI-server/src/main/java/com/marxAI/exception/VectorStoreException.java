package com.marxAI.exception;

/**
 * Thrown when a Qdrant operation fails: bootstrapping the collection on startup ({@code
 * QdrantConfig}) or upserting/searching/deleting vectors ({@code QdrantService}).
 */
public class VectorStoreException extends RuntimeException {

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
