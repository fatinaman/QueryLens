package com.querylens.backend.dto.response;

import java.util.List;

public record TableDto(
        String name,
        List<ColumnDto> columns,
        List<ForeignKeyDto> foreignKeys) {

    public TableDto {
        columns = columns == null ? List.of() : List.copyOf(columns);
        foreignKeys = foreignKeys == null ? List.of() : List.copyOf(foreignKeys);
    }
}
