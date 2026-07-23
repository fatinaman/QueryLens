package com.querylens.backend.dto.error;

import java.util.List;

public record ApiErrorResponse(String message, List<String> errors) {

    public ApiErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
