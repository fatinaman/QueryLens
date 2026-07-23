package com.querylens.backend.parser;

import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.parser.exception.SchemaParsingException;

public interface SchemaParser {

    SchemaParseResponse parse(String sql) throws SchemaParsingException;
}
