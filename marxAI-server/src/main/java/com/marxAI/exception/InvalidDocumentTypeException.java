package com.marxAI.exception;

import com.marxAI.model.enums.DocumentType;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Thrown when a document upload specifies a {@code docType} outside {@link DocumentType}. */
public class InvalidDocumentTypeException extends RuntimeException {

    public InvalidDocumentTypeException(String docType) {
        super("Invalid document type '" + docType + "'. Allowed values: " + allowedValues());
    }

    private static String allowedValues() {
        return Arrays.stream(DocumentType.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
