package com.studymind.backend.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Every error leaves the API as {"message": "..."} so the frontend can show it directly. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<Map<String, String>> body(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return body(e.getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("Validation failed");
        return body(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return body(400, "The request body is missing or not valid JSON");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, String>> typeMismatch(TypeMismatchException e) {
        return body(400, "Invalid value for '" + e.getPropertyName() + "'");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooBig(MaxUploadSizeExceededException e) {
        return body(413, "That file is too large (limit 25 MB)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        // Spring's own client errors (unknown URL, wrong HTTP method, missing parameter...) keep their 4xx status
        if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            int code = er.getStatusCode().value();
            return body(code, code == 404 ? "Not found" : code == 405 ? "That method isn't allowed here" : "Bad request");
        }
        log.error("Unhandled error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong on our side. Please try again.");
    }
}
