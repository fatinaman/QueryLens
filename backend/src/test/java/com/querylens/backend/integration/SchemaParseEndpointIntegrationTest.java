package com.querylens.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.querylens.backend.dto.request.SchemaParseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class SchemaParseEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private JsonMapper jsonMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void parsesBuiltInSchemaThroughRealEndpoint() throws Exception {
        String sql = """
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
                """;

        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(
                                new SchemaParseRequest(sql))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables.length()").value(3))
                .andExpect(jsonPath("$.tables[0].name").value("users"))
                .andExpect(jsonPath("$.tables[1].name").value("projects"))
                .andExpect(jsonPath("$.tables[2].name").value("tasks"))
                .andExpect(jsonPath("$.tables[0].columns[0].name").value("id"))
                .andExpect(jsonPath("$.tables[0].columns[0].primaryKey").value(true))
                .andExpect(jsonPath("$.tables[0].columns[0].nullable").value(false))
                .andExpect(jsonPath("$.tables[2].columns[0].name").value("id"))
                .andExpect(jsonPath("$.tables[2].columns[1].name").value("title"))
                .andExpect(jsonPath("$.tables[2].columns[2].name").value("project_id"))
                .andExpect(jsonPath("$.tables[2].columns[3].name").value("assigned_to"))
                .andExpect(jsonPath("$.tables[2].columns[3].nullable").value(true))
                .andExpect(jsonPath("$.tables[1].foreignKeys[0].column")
                        .value("owner_id"))
                .andExpect(jsonPath("$.tables[1].foreignKeys[0].referencedTable")
                        .value("users"))
                .andExpect(jsonPath("$.tables[1].foreignKeys[0].referencedColumn")
                        .value("id"))
                .andExpect(jsonPath("$.tables[2].foreignKeys[0].column")
                        .value("project_id"))
                .andExpect(jsonPath("$.tables[2].foreignKeys[0].referencedTable")
                        .value("projects"))
                .andExpect(jsonPath("$.tables[2].foreignKeys[1].column")
                        .value("assigned_to"))
                .andExpect(jsonPath("$.tables[2].foreignKeys[1].referencedTable")
                        .value("users"));
    }

    @Test
    void returnsStructuredErrorFromRealParser() throws Exception {
        mockMvc.perform(post("/api/schema/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsBytes(
                                new SchemaParseRequest("DROP TABLE users"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to parse SQL schema"))
                .andExpect(jsonPath("$.errors[0]")
                        .value("Only CREATE TABLE statements are supported"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
