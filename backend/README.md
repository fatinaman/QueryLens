# QueryLens Backend

The backend is a stateless Java 21 and Spring Boot 4.1 service that parses a
supported subset of PostgreSQL DDL into the DTO contract consumed by the
QueryLens frontend.

## Architecture

- `controller/SchemaController` validates HTTP requests.
- `service/SchemaService` coordinates schema parsing.
- `parser/SchemaParser` defines the parser boundary.
- `parser/JSqlSchemaParser` uses JSqlParser 5.3.
- `exception/GlobalExceptionHandler` returns safe structured errors.
- `config/CorsConfig` applies configurable CORS to `/api/**`.

The service has no repository layer or database because requests are parsed
entirely in memory.

## API

```http
POST /api/schema/parse
Content-Type: application/json
```

```json
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY);"
}
```

Validation and parser failures return HTTP 400 with a stable error shape:

```json
{
  "message": "Unable to parse SQL schema",
  "errors": ["Only CREATE TABLE statements are supported"]
}
```

See [the complete API documentation](../docs/API.md) for success responses,
status codes, validation limits, and examples.

## Prerequisites

- Java Development Kit 21
- PowerShell, Command Prompt, or a POSIX shell

Global Maven installation is unnecessary; use `mvnw.cmd` on Windows or
`./mvnw` on POSIX systems.

## Local development

PowerShell:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd spring-boot:run
```

The default API address is `http://localhost:8080/api/schema/parse`. A browser
GET request to `/` returns 404 because QueryLens intentionally exposes no root
endpoint.

## Environment variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP listener port |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed frontend origins |

CORS permits `POST` and `OPTIONS` for `/api/**`, permits the `Content-Type`
header, and does not permit credentials.

## Testing

Run the complete suite:

```powershell
.\mvnw.cmd clean test
```

Run focused groups:

```powershell
.\mvnw.cmd -Dtest=JSqlSchemaParserTest,SchemaParsingExceptionTest test
.\mvnw.cmd -Dtest=SchemaServiceTest,SchemaControllerTest test
```

The suite contains DTO, parser, service, controller, CORS, error-contract, and
full-context integration coverage.

## Docker

Build from the repository root:

```powershell
docker build -t querylens-backend ./backend
docker run --rm -p 8080:8080 `
  -e ALLOWED_ORIGINS=http://localhost:3000 `
  querylens-backend
```

The multi-stage Dockerfile:

- uses Java 21 for build and runtime,
- resolves dependencies through the Maven Wrapper,
- runs `clean package`, including all tests,
- copies only the application JAR into the runtime stage,
- and runs the application as a dedicated non-root user.

## Troubleshooting

- Confirm `java -version` reports 21 when Maven reports a release mismatch.
- Set `ALLOWED_ORIGINS` to the exact scheme, host, and port shown in the
  browser when CORS fails.
- Check whether port 8080 is already occupied when startup reports a bind
  failure.
- Maven Wrapper downloads require internet access on the first run.

## Supported subset and limitations

The parser supports multiple `CREATE TABLE` statements, common PostgreSQL
types and arguments, nullability, inline and table-level primary and foreign
keys, named and composite constraints, forward references, and
schema-qualified names.

It intentionally rejects or does not analyze advanced features such as
non-`CREATE TABLE` statements, `ALTER TABLE` constraints,
`CREATE TABLE AS SELECT`, inheritance, partitioning, generated-column
semantics, storage parameters, complex checks, identity behavior, and
referential actions.
