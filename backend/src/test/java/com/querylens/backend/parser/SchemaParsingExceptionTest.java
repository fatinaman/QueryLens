package com.querylens.backend.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.querylens.backend.parser.exception.SchemaParsingException;
import org.junit.jupiter.api.Test;

class SchemaParsingExceptionTest {

    @Test
    void retainsMessageAndCause() {
        var cause = new IllegalArgumentException("low-level detail");

        var exception = new SchemaParsingException("Unable to parse SQL schema", cause);

        assertThat(exception)
                .hasMessage("Unable to parse SQL schema")
                .hasCause(cause);
    }
}
