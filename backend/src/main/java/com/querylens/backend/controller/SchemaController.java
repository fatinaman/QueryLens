package com.querylens.backend.controller;

import com.querylens.backend.dto.request.SchemaParseRequest;
import com.querylens.backend.dto.response.SchemaParseResponse;
import com.querylens.backend.service.SchemaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @PostMapping(
            path = "/parse",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SchemaParseResponse> parseSchema(
            @Valid @RequestBody SchemaParseRequest request) {
        return ResponseEntity.ok(schemaService.parseSchema(request.sql()));
    }
}
