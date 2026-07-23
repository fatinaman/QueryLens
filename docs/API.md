# QueryLens Backend API

## Local base URL

```text
http://localhost:8080
```

The backend is stateless and requires no database.

## Parse a schema

```http
POST /api/schema/parse
Content-Type: application/json
```

### Request

```json
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY);"
}
```

The `sql` property is required, must not be blank, and is limited to 100,000
characters.

### Successful response

Status: `200 OK`

```json
{
  "tables": [
    {
      "name": "users",
      "columns": [
        {
          "name": "id",
          "dataType": "BIGSERIAL",
          "nullable": false,
          "primaryKey": true
        }
      ],
      "foreignKeys": []
    }
  ]
}
```

### Validation error

Status: `400 Bad Request`

```json
{
  "message": "Request validation failed",
  "errors": [
    "SQL schema must not be blank"
  ]
}
```

### Parsing error

Status: `400 Bad Request`

```json
{
  "message": "Unable to parse SQL schema",
  "errors": [
    "Only CREATE TABLE statements are supported"
  ]
}
```

### Malformed JSON

Status: `400 Bad Request`

```json
{
  "message": "Invalid request body",
  "errors": [
    "Request body must contain valid JSON"
  ]
}
```

Unexpected server failures return `500 Internal Server Error` with a generic
response that does not expose internal exception details.

## Status codes

| Status | Meaning |
| --- | --- |
| `200` | The supported schema was parsed successfully. |
| `400` | Request validation, JSON decoding, or SQL parsing failed. |
| `405` | The endpoint was called with an unsupported HTTP method. |
| `415` | The request did not use `application/json`. |
| `500` | An unexpected internal error occurred. |

## Supported SQL subset

The MVP parser supports one or more PostgreSQL-style `CREATE TABLE`
statements, declaration-ordered tables and columns, common data types and type
arguments, nullability, inline and table-level primary keys, named and
composite primary keys, inline and table-level foreign keys, named and
composite foreign keys, forward references, and schema-qualified names.

Advanced PostgreSQL features are not intentionally supported. Examples include
`ALTER TABLE` constraints, table inheritance, partitioning, generated-column
analysis, complex check constraints, storage options, identity behavior
analysis, and referential-action modeling. Non-`CREATE TABLE` statements are
rejected.

## Built-in example schema

```sql
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
```

## Windows PowerShell example

Start the backend in one PowerShell window:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd spring-boot:run
```

Create a JSON request file and call the endpoint from another window:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'

@'
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL);"
}
'@ | Set-Content -Encoding utf8 request.json

curl.exe `
  -X POST `
  -H "Content-Type: application/json" `
  --data-binary "@request.json" `
  http://localhost:8080/api/schema/parse

Remove-Item request.json
```

Stop the backend with `Ctrl+C`.
