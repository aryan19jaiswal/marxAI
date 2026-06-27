package com.marxAI.exception;

import com.marxAI.model.dto.ErrorResponse;
import dev.langchain4j.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Translates exceptions thrown by controllers/services into a uniform {@link ErrorResponse} body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "One or more fields are invalid",
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed Request", "Request body is missing or malformed", request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Email Already Exists", ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(
            DocumentNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Document Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(
            SessionNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Session Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocumentType(
            InvalidDocumentTypeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Document Type", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFileType(
            UnsupportedFileTypeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Unsupported File Type", ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentParsingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentParsing(
            DocumentParsingException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Document Parsing Failed", ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large", "Uploaded file exceeds the maximum allowed size",
                request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException ex, HttpServletRequest request) {
        log.error("Storage operation failed while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.BAD_GATEWAY, "Storage Error", "Failed to communicate with file storage", request);
    }

    @ExceptionHandler(VectorStoreException.class)
    public ResponseEntity<ErrorResponse> handleVectorStoreException(
            VectorStoreException ex, HttpServletRequest request) {
        log.error("Vector store operation failed while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.BAD_GATEWAY, "Vector Store Error", "Failed to communicate with the vector store", request);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex, HttpServletRequest request) {
        log.warn("Gemini API rate limit hit for {} {}", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded",
                "The AI service is temporarily unavailable due to rate limiting. Please try again shortly.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid Credentials", "Email or password is incorrect", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication failed", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Something went wrong. Please try again later.",
                request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
    }
}
