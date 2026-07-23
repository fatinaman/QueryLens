package com.querylens.backend.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SchemaParseRequestTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validSqlPassesValidation() {
        var request = new SchemaParseRequest("CREATE TABLE users (id BIGINT PRIMARY KEY)");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void blankSqlFailsValidation() {
        var request = new SchemaParseRequest("   ");

        Set<ConstraintViolation<SchemaParseRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("SQL schema must not be blank");
    }

    @Test
    void oversizedSqlFailsValidation() {
        var request = new SchemaParseRequest("x".repeat(100_001));

        Set<ConstraintViolation<SchemaParseRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("SQL schema must not exceed 100000 characters");
    }
}
