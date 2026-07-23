package com.querylens.backend.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.querylens.backend.dto.response.ColumnDto;
import com.querylens.backend.dto.response.ForeignKeyDto;
import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.dto.response.TableDto;
import com.querylens.backend.parser.exception.SchemaParsingException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import org.springframework.stereotype.Component;

@Component
public class JSqlSchemaParser implements SchemaParser {

    @Override
    public SchemaParseResponse parse(String sql) throws SchemaParsingException {
        if (sql == null || sql.isBlank()) {
            throw new SchemaParsingException("SQL schema must not be blank");
        }
        if (sql.replace(";", "").isBlank()) {
            throw new SchemaParsingException("No CREATE TABLE statements were found");
        }

        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (JSQLParserException exception) {
            throw new SchemaParsingException("Unable to parse SQL schema", exception);
        }

        if (statements == null || statements.isEmpty()) {
            throw new SchemaParsingException("No CREATE TABLE statements were found");
        }

        List<TableDto> tables = new ArrayList<>();
        Set<String> tableNames = new HashSet<>();
        for (Statement statement : statements) {
            if (!(statement instanceof CreateTable createTable)) {
                throw new SchemaParsingException("Only CREATE TABLE statements are supported");
            }

            TableDto table = parseCreateTable(createTable);
            if (!tableNames.add(table.name())) {
                throw new SchemaParsingException("Duplicate table name: " + table.name());
            }
            tables.add(table);
        }

        if (tables.isEmpty()) {
            throw new SchemaParsingException("No CREATE TABLE statements were found");
        }
        return new SchemaParseResponse(tables);
    }

    private TableDto parseCreateTable(CreateTable createTable) {
        rejectUnsupportedCreateTable(createTable);

        String tableName = formatTableName(createTable.getTable());
        List<ColumnDefinition> definitions = createTable.getColumnDefinitions();
        if (definitions == null || definitions.isEmpty()) {
            throw new SchemaParsingException(
                    "CREATE TABLE must declare columns for table '" + tableName + "'");
        }

        Map<String, ParsedColumn> columns = parseColumns(definitions, tableName);
        List<ForeignKeyDto> foreignKeys = new ArrayList<>();
        extractInlineForeignKeys(definitions, columns, tableName, foreignKeys);
        applyTableConstraints(createTable.getIndexes(), columns, tableName, foreignKeys);

        List<ColumnDto> columnDtos = columns.values().stream()
                .map(ParsedColumn::toDto)
                .toList();
        return new TableDto(tableName, columnDtos, foreignKeys);
    }

    private void rejectUnsupportedCreateTable(CreateTable createTable) {
        if (createTable.getSelect() != null) {
            throw new SchemaParsingException("CREATE TABLE AS SELECT is not supported");
        }
        if (createTable.getLikeTable() != null) {
            throw new SchemaParsingException("CREATE TABLE LIKE is not supported");
        }
    }

    private Map<String, ParsedColumn> parseColumns(
            List<ColumnDefinition> definitions,
            String tableName) {
        Map<String, ParsedColumn> columns = new LinkedHashMap<>();
        for (ColumnDefinition definition : definitions) {
            String columnName = unquoteIdentifier(definition.getColumnName());
            if (columns.containsKey(columnName)) {
                throw new SchemaParsingException(
                        "Duplicate column name '" + columnName + "' in table '" + tableName + "'");
            }

            boolean primaryKey = containsSequence(
                    definition.getColumnSpecs(), "PRIMARY", "KEY");
            boolean nullable = !primaryKey
                    && !containsSequence(definition.getColumnSpecs(), "NOT", "NULL");
            columns.put(columnName, new ParsedColumn(
                    columnName,
                    formatDataType(definition),
                    nullable,
                    primaryKey));
        }
        return columns;
    }

    private void extractInlineForeignKeys(
            List<ColumnDefinition> definitions,
            Map<String, ParsedColumn> columns,
            String tableName,
            List<ForeignKeyDto> foreignKeys) {
        for (ColumnDefinition definition : definitions) {
            List<String> specs = definition.getColumnSpecs();
            int referencesIndex = indexOf(specs, "REFERENCES");
            if (referencesIndex < 0) {
                continue;
            }

            if (referencesIndex + 2 >= specs.size()) {
                throw new SchemaParsingException(
                        "Unable to parse foreign key for column '"
                                + unquoteIdentifier(definition.getColumnName())
                                + "' in table '" + tableName + "'");
            }

            String sourceColumn = unquoteIdentifier(definition.getColumnName());
            ensureSourceColumnExists(columns, sourceColumn, tableName);
            String referencedTable = unquoteQualifiedName(specs.get(referencesIndex + 1));
            List<String> referencedColumns = parseParenthesizedIdentifiers(
                    specs, referencesIndex + 2);
            if (referencedColumns.size() != 1) {
                throw new SchemaParsingException(
                        "Inline foreign key must reference exactly one column");
            }
            foreignKeys.add(new ForeignKeyDto(
                    sourceColumn, referencedTable, referencedColumns.getFirst()));
        }
    }

    private void applyTableConstraints(
            List<Index> indexes,
            Map<String, ParsedColumn> columns,
            String tableName,
            List<ForeignKeyDto> foreignKeys) {
        if (indexes == null) {
            return;
        }

        for (Index index : indexes) {
            if (index instanceof ForeignKeyIndex foreignKey) {
                addTableForeignKeys(foreignKey, columns, tableName, foreignKeys);
            } else if ("PRIMARY KEY".equalsIgnoreCase(index.getType())) {
                applyPrimaryKey(index, columns, tableName);
            }
        }
    }

    private void applyPrimaryKey(
            Index primaryKey,
            Map<String, ParsedColumn> columns,
            String tableName) {
        for (String rawColumn : safeList(primaryKey.getColumnsNames())) {
            String columnName = unquoteIdentifier(rawColumn);
            ParsedColumn column = columns.get(columnName);
            if (column == null) {
                throw new SchemaParsingException(
                        "Primary key references unknown column '" + columnName
                                + "' in table '" + tableName + "'");
            }
            column.makePrimaryKey();
        }
    }

    private void addTableForeignKeys(
            ForeignKeyIndex foreignKey,
            Map<String, ParsedColumn> columns,
            String tableName,
            List<ForeignKeyDto> foreignKeys) {
        List<String> sourceColumns = safeList(foreignKey.getColumnsNames()).stream()
                .map(this::unquoteIdentifier)
                .toList();
        List<String> referencedColumns = safeList(
                foreignKey.getReferencedColumnNames()).stream()
                .map(this::unquoteIdentifier)
                .toList();

        if (sourceColumns.size() != referencedColumns.size()) {
            throw new SchemaParsingException(
                    "Foreign key column count does not match referenced column count");
        }

        String referencedTable = formatTableName(foreignKey.getTable());
        for (int index = 0; index < sourceColumns.size(); index++) {
            String sourceColumn = sourceColumns.get(index);
            ensureSourceColumnExists(columns, sourceColumn, tableName);
            foreignKeys.add(new ForeignKeyDto(
                    sourceColumn, referencedTable, referencedColumns.get(index)));
        }
    }

    private void ensureSourceColumnExists(
            Map<String, ParsedColumn> columns,
            String columnName,
            String tableName) {
        if (!columns.containsKey(columnName)) {
            throw new SchemaParsingException(
                    "Foreign key references unknown source column '" + columnName
                            + "' in table '" + tableName + "'");
        }
    }

    private String formatDataType(ColumnDefinition definition) {
        if (definition.getColDataType() == null) {
            throw new SchemaParsingException(
                    "Column '" + unquoteIdentifier(definition.getColumnName())
                            + "' must declare a data type");
        }
        return definition.getColDataType().toString()
                .trim()
                .replaceAll("\\s+\\(", "(")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String formatTableName(Table table) {
        if (table == null || table.getName() == null || table.getName().isBlank()) {
            throw new SchemaParsingException("CREATE TABLE must declare a table name");
        }
        String tableName = unquoteIdentifier(table.getUnquotedName());
        String schemaName = unquoteIdentifier(table.getUnquotedSchemaName());
        if (schemaName.isBlank()) {
            return tableName;
        }
        return schemaName + "." + tableName;
    }

    private boolean containsSequence(List<String> tokens, String first, String second) {
        if (tokens == null) {
            return false;
        }
        for (int index = 0; index < tokens.size() - 1; index++) {
            if (first.equalsIgnoreCase(tokens.get(index))
                    && second.equalsIgnoreCase(tokens.get(index + 1))) {
                return true;
            }
        }
        return false;
    }

    private int indexOf(List<String> tokens, String expected) {
        if (tokens == null) {
            return -1;
        }
        for (int index = 0; index < tokens.size(); index++) {
            if (expected.equalsIgnoreCase(tokens.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private List<String> parseParenthesizedIdentifiers(
            List<String> tokens,
            int startIndex) {
        StringBuilder value = new StringBuilder();
        for (int index = startIndex; index < tokens.size(); index++) {
            if (!value.isEmpty()) {
                value.append(' ');
            }
            value.append(tokens.get(index));
            if (tokens.get(index).contains(")")) {
                break;
            }
        }

        String text = value.toString().trim();
        int opening = text.indexOf('(');
        int closing = text.indexOf(')', opening + 1);
        if (opening < 0 || closing < 0) {
            throw new SchemaParsingException("Unable to parse referenced columns");
        }

        String contents = text.substring(opening + 1, closing);
        if (contents.isBlank()) {
            return List.of();
        }
        return List.of(contents.split(",")).stream()
                .map(String::trim)
                .map(this::unquoteIdentifier)
                .toList();
    }

    private String unquoteQualifiedName(String name) {
        return List.of(name.split("\\.")).stream()
                .map(this::unquoteIdentifier)
                .reduce((left, right) -> left + "." + right)
                .orElse(name);
    }

    private String unquoteIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '`' && last == '`')
                    || (first == '[' && last == ']')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static final class ParsedColumn {

        private final String name;
        private final String dataType;
        private boolean nullable;
        private boolean primaryKey;

        private ParsedColumn(
                String name,
                String dataType,
                boolean nullable,
                boolean primaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        private void makePrimaryKey() {
            primaryKey = true;
            nullable = false;
        }

        private ColumnDto toDto() {
            return new ColumnDto(name, dataType, nullable, primaryKey);
        }
    }
}
