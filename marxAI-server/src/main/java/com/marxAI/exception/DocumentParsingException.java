package com.marxAI.exception;

/**
 * Thrown when {@code TikaDocumentParser} cannot extract text from an uploaded file, e.g. the bytes
 * are corrupt, password-protected, or not actually the format their extension claims.
 */
public class DocumentParsingException extends RuntimeException {

    public DocumentParsingException(String filename, Throwable cause) {
        super("Failed to extract text from '" + filename + "'", cause);
    }
}
