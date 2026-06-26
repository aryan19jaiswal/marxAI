package com.marxAI.exception;

import java.util.UUID;

/** Thrown when a document id resolves to no row in the {@code documents} table. */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID documentId) {
        super("Document not found: " + documentId);
    }
}
