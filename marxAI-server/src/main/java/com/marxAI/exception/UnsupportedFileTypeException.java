package com.marxAI.exception;

import java.util.Set;

/** Thrown when an uploaded file's extension is outside the supported set (PDF, MD, TXT). */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String filename, Set<String> allowedExtensions) {
        super("Unsupported file type for '" + filename + "'. Allowed extensions: " + allowedExtensions);
    }
}
