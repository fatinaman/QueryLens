package com.querylens.backend.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.querylens.backend.dto.response.ColumnDto;
import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.dto.response.TableDto;
import com.querylens.backend.exception.GlobalExceptionHandler;
import com.querylens.backend.parser.exception.SchemaParsingException;
import com.querylens.backend.service.SchemaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SchemaControllerTest {

    private SchemaService schemaService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        schemaService = mock(SchemaService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SchemaController(schemaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void validRequestReturnsRequiredJsonAndDelegatesSql() throws Exception {
        String sql = "CREATE TABLE users (id BIGINT PRIMARY KEY)";
        var response = new SchemaParseResponse(List.of(
                new TableDto(
                        "users",
                        List.of(new ColumnDto("id", "BIGINT", false, true)),
                        List.of())));
        when(schemaService.parseSchema(sql)).thenReturn(response);

        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sql":"CREATE TABLE users (id BIGINT PRIMARY KEY)"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tables[0].name").value("users"))
                .andExpect(jsonPath("$.tables[0].columns[0].name").value("id"))
                .andExpect(jsonPath("$.tables[0].columns[0].dataType").value("BIGINT"))
                .andExpect(jsonPath("$.tables[0].columns[0].nullable").value(false))
                .andExpect(jsonPath("$.tables[0].columns[0].primaryKey").value(true))
                .andExpect(jsonPath("$.tables[0].foreignKeys").isArray());

        verify(schemaService).parseSchema(sql);
    }

    @Test
    void blankMissingAndNullSqlReturnValidationErrors() throws Exception {
        for (String body : List.of(
                "{\"sql\":\"   \"}",
                "{}",
                "{\"sql\":null}")) {
            mockMvc.perform(post("/api/schema/parse")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Request validation failed"))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[0]")
                            .value("SQL schema must not be blank"))
                    .andExpect(jsonPath("$.stackTrace").doesNotExist());
        }
    }

    @Test
    void oversizedSqlReturnsValidationError() throws Exception {
        String body = "{\"sql\":\"" + "x".repeat(100_001) + "\"}";

        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("SQL schema must not exceed 100000 characters"));
    }

    @Test
    void malformedJsonReturnsSafeStructuredError() throws Exception {
        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("Request body must contain valid JSON"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void parserFailureReturnsSafeStructuredError() throws Exception {
        when(schemaService.parseSchema(anyString())).thenThrow(
                new SchemaParsingException(
                        "Only CREATE TABLE statements are supported"));

        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"DROP TABLE users\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to parse SQL schema"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("Only CREATE TABLE statements are supported"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void unexpectedFailureReturnsInternalErrorWithoutLeakingMessage() throws Exception {
        when(schemaService.parseSchema(anyString())).thenThrow(
                new IllegalStateException("sensitive internal detail"));

        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\":\"CREATE TABLE users (id BIGINT)\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("An unexpected error occurred"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "sensitive internal detail"))));
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/schema/parse"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void incorrectContentTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("CREATE TABLE users (id BIGINT)"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
