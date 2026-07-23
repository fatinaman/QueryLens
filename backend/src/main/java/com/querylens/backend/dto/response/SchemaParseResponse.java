package com.querylens.backend.dto.response;

import java.util.List;

public record SchemaParseResponse(List<TableDto> tables) {

    public SchemaParseResponse {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
