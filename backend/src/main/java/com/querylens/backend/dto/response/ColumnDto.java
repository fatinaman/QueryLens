package com.querylens.backend.dto.response;

public record ColumnDto(
        String name,
        String dataType,
        boolean nullable,
        boolean primaryKey) {
}
