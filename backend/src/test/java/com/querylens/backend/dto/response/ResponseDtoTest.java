package com.querylens.backend.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ResponseDtoTest {

    @Test
    void tableConvertsNullCollectionsToEmptyImmutableLists() {
        var table = new TableDto("users", null, null);

        assertThat(table.columns()).isEmpty();
        assertThat(table.foreignKeys()).isEmpty();
        assertThatThrownBy(() -> table.columns().add(
                new ColumnDto("id", "BIGINT", false, true)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> table.foreignKeys().add(
                new ForeignKeyDto("user_id", "users", "id")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tableDefensivelyCopiesCollectionsAndPreservesOrder() {
        var id = new ColumnDto("id", "BIGINT", false, true);
        var email = new ColumnDto("email", "TEXT", false, false);
        var foreignKey = new ForeignKeyDto("account_id", "accounts", "id");
        var columns = new ArrayList<>(List.of(id, email));
        var foreignKeys = new ArrayList<>(List.of(foreignKey));

        var table = new TableDto("users", columns, foreignKeys);
        columns.clear();
        foreignKeys.clear();

        assertThat(table.columns()).containsExactly(id, email);
        assertThat(table.foreignKeys()).containsExactly(foreignKey);
        assertThatThrownBy(() -> table.columns().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> table.foreignKeys().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void schemaResponseDefensivelyCopiesTableList() {
        var users = new TableDto("users", List.of(), List.of());
        var accounts = new TableDto("accounts", List.of(), List.of());
        var tables = new ArrayList<>(List.of(users, accounts));

        var response = new SchemaParseResponse(tables);
        tables.clear();

        assertThat(response.tables()).containsExactly(users, accounts);
        assertThatThrownBy(() -> response.tables().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
