package com.querylens.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.querylens.backend.dto.error.ApiErrorResponse;
import com.querylens.backend.dto.request.SchemaParseRequest;
import com.querylens.backend.dto.response.ColumnDto;
import com.querylens.backend.dto.response.ForeignKeyDto;
import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.dto.response.TableDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DtoJsonTest {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void serializesDtoContractWithRequiredPropertyNames() throws Exception {
        var response = new SchemaParseResponse(List.of(
                new TableDto(
                        "users",
                        List.of(new ColumnDto("id", "BIGINT", false, true)),
                        List.of(new ForeignKeyDto("account_id", "accounts", "id")))));

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).isEqualTo(
                """
                {"tables":[{"name":"users","columns":[{"name":"id","dataType":"BIGINT","nullable":false,"primaryKey":true}],"foreignKeys":[{"column":"account_id","referencedTable":"accounts","referencedColumn":"id"}]}]}\
                """);
    }

    @Test
    void deserializesRequestWithoutModifyingSql() throws Exception {
        String json = """
                {"sql":"  CREATE TABLE users (id BIGINT)  "}
                """;

        SchemaParseRequest request = objectMapper.readValue(json, SchemaParseRequest.class);

        assertThat(request.sql()).isEqualTo("  CREATE TABLE users (id BIGINT)  ");
    }

    @Test
    void deserializesResponseAndErrorDtos() throws Exception {
        String responseJson = """
                {
                  "tables": [{
                    "name": "users",
                    "columns": [{
                      "name": "id",
                      "dataType": "BIGINT",
                      "nullable": false,
                      "primaryKey": true
                    }],
                    "foreignKeys": []
                  }]
                }
                """;
        String errorJson = """
                {
                  "message": "Unable to parse SQL schema",
                  "errors": ["Specific useful parsing error"]
                }
                """;

        SchemaParseResponse response = objectMapper.readValue(
                responseJson, SchemaParseResponse.class);
        ApiErrorResponse error = objectMapper.readValue(
                errorJson, ApiErrorResponse.class);

        assertThat(response.tables()).singleElement().satisfies(table -> {
            assertThat(table.name()).isEqualTo("users");
            assertThat(table.columns()).containsExactly(
                    new ColumnDto("id", "BIGINT", false, true));
            assertThat(table.foreignKeys()).isEmpty();
        });
        assertThat(error).isEqualTo(new ApiErrorResponse(
                "Unable to parse SQL schema",
                List.of("Specific useful parsing error")));
    }
}
