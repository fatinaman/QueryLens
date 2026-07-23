package com.querylens.backend.dto.response;

public record ForeignKeyDto(
        String column,
        String referencedTable,
        String referencedColumn) {
}
