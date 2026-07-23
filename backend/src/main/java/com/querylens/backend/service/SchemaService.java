package com.querylens.backend.service;

import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.parser.SchemaParser;
import org.springframework.stereotype.Service;

@Service
public class SchemaService {

    private final SchemaParser schemaParser;

    public SchemaService(SchemaParser schemaParser) {
        this.schemaParser = schemaParser;
    }

    public SchemaParseResponse parseSchema(String sql) {
        return schemaParser.parse(sql);
    }
}
