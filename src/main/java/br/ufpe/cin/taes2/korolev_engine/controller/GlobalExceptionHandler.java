package br.ufpe.cin.taes2.korolev_engine.controller;

import br.ufpe.cin.taes2.korolev_engine.controller.dto.ErrorResponse;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagAlreadyExistsException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagNotFoundException;
import br.ufpe.cin.taes2.korolev_engine.domain.exception.FeatureFlagValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeatureFlagException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(FeatureFlagException ex) {
        log.warn("[GlobalExceptionHandler] - Intercepting domain exception - Intercepted FeatureFlagException: {}", ex.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = "VALIDATION_ERROR";

        if (ex instanceof FeatureFlagAlreadyExistsException) {
            status = HttpStatus.CONFLICT;
            errorCode = "RESOURCE_ALREADY_EXISTS";
        } else if (ex instanceof FeatureFlagNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorCode = "RESOURCE_NOT_FOUND";
        } else if (ex instanceof FeatureFlagValidationException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "VALIDATION_ERROR";
        }

        ErrorResponse.ErrorResponseBuilder responseBuilder = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now());

        if (ex instanceof FeatureFlagValidationException) {
            FeatureFlagValidationException validationEx = (FeatureFlagValidationException) ex;
            if (validationEx.getErrors() != null && !validationEx.getErrors().isEmpty()) {
                responseBuilder.errors(validationEx.getErrors());
            }
        }

        return ResponseEntity.status(status).body(responseBuilder.build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[GlobalExceptionHandler] - Intercepting illegal argument exception - Intercepted IllegalArgumentException: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
