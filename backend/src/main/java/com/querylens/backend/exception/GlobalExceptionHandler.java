package com.querylens.backend.exception;

import java.util.LinkedHashSet;
import java.util.List;

import com.querylens.backend.dto.error.ApiErrorResponse;
import com.querylens.backend.parser.exception.SchemaParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SchemaParsingException.class)
    public ResponseEntity<ApiErrorResponse> handleSchemaParsing(
            SchemaParsingException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = "Unable to parse SQL schema";
        }
        var response = new ApiErrorResponse(
                "Unable to parse SQL schema",
                List.of(detail));
        return ResponseEntity.badRequest().body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var messages = new LinkedHashSet<String>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String message = error.getDefaultMessage();
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
        });
        var response = new ApiErrorResponse(
                "Request validation failed",
                List.copyOf(messages));
        return handleExceptionInternal(
                exception, response, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var response = new ApiErrorResponse(
                "Invalid request body",
                List.of("Request body must contain valid JSON"));
        return handleExceptionInternal(
                exception, response, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        logger.error("Unexpected error while processing request", exception);
        var response = new ApiErrorResponse(
                "Internal server error",
                List.of("An unexpected error occurred"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
