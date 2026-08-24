package com.kniazev.cards.word.error.handler;

import jakarta.persistence.EntityNotFoundException;

import com.kniazev.cards.word.error.exception.WordCardException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(WordCardException.class)
    public ResponseEntity<String> handleWordCardException(WordCardException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /**
     * A missing static resource is just a 404. Browsers and internet scanners
     * constantly probe a public server for files like robots.txt or random *.js,
     * so return 404 quietly instead of an ERROR-level stacktrace that spams the log.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("No static resource: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    /** Preserve the intended status of explicitly-thrown status exceptions (e.g. 401/403). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error(e.getMessage(), e);

        return ResponseEntity.badRequest().build();
    }
}
