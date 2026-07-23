package com.querylens.backend.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.querylens.backend.dto.response.ColumnDto;
import com.querylens.backend.dto.response.ForeignKeyDto;
import com.querylens.backend.parser.exception.SchemaParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JSqlSchemaParserTest {

    private JSqlSchemaParser parser;

    @BeforeEach
    void createParser() {
        parser = new JSqlSchemaParser();
    }

    @Test
    void parsesBasicTableAndPreservesColumnOrderAndNullability() {
        var response = parser.parse("""
                CREATE TABLE users (
                    id BIGINT,
                    display_name VARCHAR(100) NOT NULL,
                    email TEXT
                );
                """);

        assertThat(response.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("users");
            assertThat(table.columns()).containsExactly(
                    new ColumnDto("id", "BIGINT", true, false),
                    new ColumnDto("display_name", "VARCHAR(100)", false, false),
                    new ColumnDto("email", "TEXT", true, false));
            assertThat(table.foreignKeys()).isEmpty();
        });
    }

    @Test
    void parsesMultipleTablesInDeclarationOrder() {
        var response = parser.parse("""
                CREATE TABLE users (id BIGINT);
                CREATE TABLE projects (id BIGINT);
                CREATE TABLE tasks (id BIGINT);
                """);

        assertThat(response.tables())
                .extracting(table -> table.name())
                .containsExactly("users", "projects", "tasks");
    }

    @Test
    void formatsCommonPostgresqlDataTypesAndArguments() {
        var response = parser.parse("""
                CREATE TABLE types (
                    bigint_value BIGINT,
                    bigserial_value BIGSERIAL,
                    varchar_value VARCHAR(100),
                    varying_value CHARACTER VARYING(255),
                    numeric_value NUMERIC(10, 2),
                    boolean_value BOOLEAN,
                    timestamp_value TIMESTAMP,
                    timestamp_zone_value TIMESTAMP WITHOUT TIME ZONE,
                    uuid_value UUID,
                    text_value TEXT,
                    date_value DATE,
                    integer_value INTEGER,
                    double_value DOUBLE PRECISION
                );
                """);

        assertThat(response.tables().getFirst().columns())
                .extracting(ColumnDto::dataType)
                .containsExactly(
                        "BIGINT",
                        "BIGSERIAL",
                        "VARCHAR(100)",
                        "CHARACTER VARYING(255)",
                        "NUMERIC(10, 2)",
                        "BOOLEAN",
                        "TIMESTAMP",
                        "TIMESTAMP WITHOUT TIME ZONE",
                        "UUID",
                        "TEXT",
                        "DATE",
                        "INTEGER",
                        "DOUBLE PRECISION");
    }

    @Test
    void parsesInlinePrimaryKeyAndMakesItNonNullable() {
        var column = parser.parse(
                "CREATE TABLE users (id BIGSERIAL PRIMARY KEY)")
                .tables().getFirst().columns().getFirst();

        assertThat(column).isEqualTo(new ColumnDto("id", "BIGSERIAL", false, true));
    }

    @Test
    void parsesTableLevelNamedAndCompositePrimaryKeys() {
        var unnamed = parser.parse("""
                CREATE TABLE memberships (
                    project_id BIGINT,
                    user_id BIGINT,
                    PRIMARY KEY (project_id, user_id)
                )
                """).tables().getFirst();
        var named = parser.parse("""
                CREATE TABLE users (
                    id BIGINT,
                    CONSTRAINT pk_users PRIMARY KEY (id)
                )
                """).tables().getFirst();

        assertThat(unnamed.columns())
                .allSatisfy(column -> {
                    assertThat(column.primaryKey()).isTrue();
                    assertThat(column.nullable()).isFalse();
                });
        assertThat(named.columns().getFirst())
                .isEqualTo(new ColumnDto("id", "BIGINT", false, true));
    }

    @Test
    void parsesInlineAndTableLevelForeignKeysInOrder() {
        var table = parser.parse("""
                CREATE TABLE tasks (
                    id BIGINT,
                    project_id BIGINT REFERENCES projects(id),
                    assigned_to BIGINT REFERENCES users(id),
                    owner_id BIGINT,
                    FOREIGN KEY (owner_id) REFERENCES users(id)
                )
                """).tables().getFirst();

        assertThat(table.foreignKeys()).containsExactly(
                new ForeignKeyDto("project_id", "projects", "id"),
                new ForeignKeyDto("assigned_to", "users", "id"),
                new ForeignKeyDto("owner_id", "users", "id"));
    }

    @Test
    void parsesNamedAndCompositeForeignKeysIntoOrderedPairs() {
        var table = parser.parse("""
                CREATE TABLE task_assignments (
                    project_id BIGINT,
                    tenant_id BIGINT,
                    owner_id BIGINT,
                    CONSTRAINT fk_project
                        FOREIGN KEY (project_id, tenant_id)
                        REFERENCES projects(id, tenant_id),
                    CONSTRAINT fk_owner
                        FOREIGN KEY (owner_id)
                        REFERENCES users(id)
                )
                """).tables().getFirst();

        assertThat(table.foreignKeys()).containsExactly(
                new ForeignKeyDto("project_id", "projects", "id"),
                new ForeignKeyDto("tenant_id", "projects", "tenant_id"),
                new ForeignKeyDto("owner_id", "users", "id"));
    }

    @Test
    void allowsForwardReferences() {
        var response = parser.parse("""
                CREATE TABLE tasks (
                    project_id BIGINT REFERENCES projects(id)
                );
                CREATE TABLE projects (id BIGINT PRIMARY KEY);
                """);

        assertThat(response.tables().getFirst().foreignKeys()).containsExactly(
                new ForeignKeyDto("project_id", "projects", "id"));
    }

    @Test
    void parsesBuiltInQueryLensExample() {
        var response = parser.parse("""
                CREATE TABLE users (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(255) NOT NULL
                );

                CREATE TABLE projects (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    owner_id BIGINT NOT NULL,
                    CONSTRAINT fk_project_owner
                        FOREIGN KEY (owner_id)
                        REFERENCES users(id)
                );

                CREATE TABLE tasks (
                    id BIGSERIAL PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    project_id BIGINT NOT NULL REFERENCES projects(id),
                    assigned_to BIGINT REFERENCES users(id)
                );
                """);

        assertThat(response.tables())
                .extracting(table -> table.name())
                .containsExactly("users", "projects", "tasks");
        assertThat(response.tables().get(0).columns()).containsExactly(
                new ColumnDto("id", "BIGSERIAL", false, true),
                new ColumnDto("name", "VARCHAR(100)", false, false),
                new ColumnDto("email", "VARCHAR(255)", false, false));
        assertThat(response.tables().get(1).foreignKeys()).containsExactly(
                new ForeignKeyDto("owner_id", "users", "id"));
        assertThat(response.tables().get(2).columns())
                .extracting(ColumnDto::name)
                .containsExactly("id", "title", "project_id", "assigned_to");
        assertThat(response.tables().get(2).columns().get(3).nullable()).isTrue();
        assertThat(response.tables().get(2).foreignKeys()).containsExactly(
                new ForeignKeyDto("project_id", "projects", "id"),
                new ForeignKeyDto("assigned_to", "users", "id"));
    }

    @Test
    void handlesSchemaQualifiedAndQuotedNamesConsistently() {
        var response = parser.parse("""
                CREATE TABLE "app"."UserAccounts" (
                    "id" BIGINT PRIMARY KEY
                );
                CREATE TABLE app.sessions (
                    account_id BIGINT,
                    FOREIGN KEY (account_id) REFERENCES "app"."UserAccounts"("id")
                );
                """);

        assertThat(response.tables())
                .extracting(table -> table.name())
                .containsExactly("app.UserAccounts", "app.sessions");
        assertThat(response.tables().get(1).foreignKeys()).containsExactly(
                new ForeignKeyDto("account_id", "app.UserAccounts", "id"));
    }

    @Test
    void rejectsNullBlankAndEmptyStatementInput() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("SQL schema must not be blank");
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("SQL schema must not be blank");
        assertThatThrownBy(() -> parser.parse(";"))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("No CREATE TABLE statements were found");
    }

    @Test
    void rejectsMalformedSqlWithoutExposingParserDetails() {
        assertThatThrownBy(() -> parser.parse("CREATE TABLE users (id BIGINT"))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("Unable to parse SQL schema")
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void rejectsUnsupportedAndMixedStatements() {
        for (String sql : List.of(
                "DROP TABLE users",
                "CREATE VIEW active_users AS SELECT * FROM users",
                "INSERT INTO users VALUES (1)",
                "CREATE TABLE users (id BIGINT); DROP TABLE old_users")) {
            assertThatThrownBy(() -> parser.parse(sql))
                    .isInstanceOf(SchemaParsingException.class)
                    .hasMessage("Only CREATE TABLE statements are supported");
        }
    }

    @Test
    void rejectsDuplicateTableAndColumnNames() {
        assertThatThrownBy(() -> parser.parse("""
                CREATE TABLE users (id BIGINT);
                CREATE TABLE users (other_id BIGINT);
                """))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("Duplicate table name: users");
        assertThatThrownBy(() -> parser.parse(
                "CREATE TABLE users (id BIGINT, id TEXT)"))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("Duplicate column name 'id' in table 'users'");
    }

    @Test
    void rejectsPrimaryKeyThatReferencesUnknownColumn() {
        assertThatThrownBy(() -> parser.parse("""
                CREATE TABLE users (
                    id BIGINT,
                    PRIMARY KEY (missing_id)
                )
                """))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage(
                        "Primary key references unknown column 'missing_id' in table 'users'");
    }

    @Test
    void rejectsForeignKeyThatReferencesUnknownSourceColumn() {
        assertThatThrownBy(() -> parser.parse("""
                CREATE TABLE projects (
                    id BIGINT,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage(
                        "Foreign key references unknown source column 'user_id' in table 'projects'");
    }

    @Test
    void rejectsMismatchedCompositeForeignKeyColumns() {
        assertThatThrownBy(() -> parser.parse("""
                CREATE TABLE assignments (
                    project_id BIGINT,
                    tenant_id BIGINT,
                    FOREIGN KEY (project_id, tenant_id) REFERENCES projects(id)
                )
                """))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("Foreign key column count does not match referenced column count");
    }

    @Test
    void rejectsCreateTableAsSelectAndLike() {
        assertThatThrownBy(() -> parser.parse(
                "CREATE TABLE archived_users AS SELECT * FROM users"))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("CREATE TABLE AS SELECT is not supported");
        assertThatThrownBy(() -> parser.parse(
                "CREATE TABLE copied_users LIKE users"))
                .isInstanceOf(SchemaParsingException.class)
                .hasMessage("CREATE TABLE LIKE is not supported");
    }
}
