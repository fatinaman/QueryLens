# QueryLens Backend

The QueryLens backend is a minimal Spring Boot application that will eventually
accept PostgreSQL schema definitions and return data for an interactive ER
diagram.

## Current status

Milestones 2–4 are complete and Milestones 5–6 are in progress while the
current work remains uncommitted. The runnable backend, immutable API DTO
contract, SQL parser, service, REST endpoint, centralized error handling, and
configurable CORS now exist. No database integration is required.

## API DTO contract

DTOs are Java records organized under `com.querylens.backend.dto`:

- `request` contains `SchemaParseRequest`.
- `response` contains table, column, foreign-key, and schema response records.
- `error` contains `ApiErrorResponse`.

The future request body will contain an `sql` string. Bean Validation rejects
blank SQL and payloads longer than 100,000 characters. A future successful
response will contain a `tables` list with columns and foreign keys; a future
error response will contain a message and list of errors.

The DTO contract is exposed by `POST /api/schema/parse`.

## SQL parser

The parser uses JSqlParser 5.3 and is organized under
`com.querylens.backend.parser`:

- `SchemaParser` defines the `parse(String sql)` contract.
- `JSqlSchemaParser` is the Spring component that interprets JSqlParser's
  syntax tree and returns the existing response DTOs.
- `exception/SchemaParsingException` wraps parsing and structural failures
  without exposing JSqlParser exceptions from the parser API.

The MVP parser supports multiple `CREATE TABLE` statements, common PostgreSQL
data types and type arguments, nullability, inline and table-level primary
keys, named constraints, inline and table-level foreign keys, composite keys,
forward references, and schema-qualified table names. Declaration ordering is
preserved.

It rejects non-`CREATE TABLE` statements, malformed SQL, duplicate tables or
columns, unknown primary-key or foreign-key source columns, and mismatched
composite foreign keys.

Advanced PostgreSQL features are not intentionally supported, including
`ALTER TABLE` constraints, `CREATE TABLE AS SELECT`, table inheritance,
partitioning, generated-column analysis, complex check constraints, storage
parameters, identity behavior analysis, and referential-action modeling.

The parser is exposed only through the schema parsing endpoint.

## Backend layers

- `controller/SchemaController` accepts and validates JSON requests.
- `service/SchemaService` delegates SQL strings to `SchemaParser`.
- `exception/GlobalExceptionHandler` produces safe structured API errors.
- `config/CorsConfig` configures CORS for `/api/**`.

## Endpoint

```http
POST /api/schema/parse
Content-Type: application/json
```

Example request:

```json
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY);"
}
```

Validation and parsing failures return HTTP 400:

```json
{
  "message": "Unable to parse SQL schema",
  "errors": [
    "Only CREATE TABLE statements are supported"
  ]
}
```

See [the API documentation](../docs/API.md) for complete response examples and
status codes.

## Prerequisites

- A Java 21 JDK
- Windows PowerShell

Maven does not need to be installed globally because the repository includes
the Maven Wrapper.

## Run the backend

From Windows PowerShell:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd spring-boot:run
```

The backend listens on port `8080` by default. Set the `SERVER_PORT`
environment variable to override it.

Browsing to `http://localhost:8080` may return the default 404 response because
there is no root endpoint. The application endpoint is
`POST http://localhost:8080/api/schema/parse`.

## Environment variables

- `SERVER_PORT` changes the default port from `8080`.
- `ALLOWED_ORIGINS` accepts one or more comma-separated CORS origins and
  defaults to `http://localhost:5173`.

CORS applies only to `/api/**`, permits `POST` and `OPTIONS`, and does not allow
credentials.

## Run the tests

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd test
```

Run only the parser tests with:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd -Dtest=JSqlSchemaParserTest,SchemaParsingExceptionTest test
```

Run the service and controller tests with:

```powershell
.\mvnw.cmd -Dtest=SchemaServiceTest,SchemaControllerTest test
```

## PowerShell API request

```powershell
@'
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY);"
}
'@ | Set-Content -Encoding utf8 request.json

curl.exe `
  -X POST `
  -H "Content-Type: application/json" `
  --data-binary "@request.json" `
  http://localhost:8080/api/schema/parse

Remove-Item request.json
```

The backend requires no database.
