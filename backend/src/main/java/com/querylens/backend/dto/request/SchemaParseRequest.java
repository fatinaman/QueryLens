package com.querylens.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchemaParseRequest(
        @NotBlank(message = "SQL schema must not be blank")
        @Size(max = 100_000, message = "SQL schema must not exceed 100000 characters")
        String sql) {
}
