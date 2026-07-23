package com.querylens.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.parser.SchemaParser;
import com.querylens.backend.parser.exception.SchemaParsingException;
import org.junit.jupiter.api.Test;

class SchemaServiceTest {

    @Test
    void delegatesSqlExactlyOnceAndReturnsParserResponse() {
        SchemaParser parser = mock(SchemaParser.class);
        var expected = new SchemaParseResponse(List.of());
        var service = new SchemaService(parser);
        String sql = "CREATE TABLE users (id BIGINT)";
        when(parser.parse(sql)).thenReturn(expected);

        SchemaParseResponse actual = service.parseSchema(sql);

        assertThat(actual).isSameAs(expected);
        verify(parser, times(1)).parse(sql);
    }

    @Test
    void propagatesSchemaParsingException() {
        SchemaParser parser = mock(SchemaParser.class);
        var service = new SchemaService(parser);
        String sql = "DROP TABLE users";
        var failure = new SchemaParsingException(
                "Only CREATE TABLE statements are supported");
        when(parser.parse(sql)).thenThrow(failure);

        assertThatThrownBy(() -> service.parseSchema(sql)).isSameAs(failure);
        verify(parser, times(1)).parse(sql);
    }
}
