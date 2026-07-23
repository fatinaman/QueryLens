package com.querylens.backend.parser.exception;

public class SchemaParsingException extends RuntimeException {

    public SchemaParsingException(String message) {
        super(message);
    }

    public SchemaParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
