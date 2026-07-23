# QueryLens Backend

The QueryLens backend is a minimal Spring Boot application that will eventually
accept PostgreSQL schema definitions and return data for an interactive ER
diagram.

## Current status

Milestone 2 is complete and Milestone 3 is in progress. The runnable backend
skeleton, application context test, and immutable API DTO contract now exist.
No API endpoint, SQL parser, or database integration has been implemented.

## API DTO contract

DTOs are Java records organized under `com.querylens.backend.dto`:

- `request` contains `SchemaParseRequest`.
- `response` contains table, column, foreign-key, and schema response records.
- `error` contains `ApiErrorResponse`.

The future request body will contain an `sql` string. Bean Validation rejects
blank SQL and payloads longer than 100,000 characters. A future successful
response will contain a `tables` list with columns and foreign keys; a future
error response will contain a message and list of errors.

The DTO contract exists, but there is no HTTP endpoint to receive or return
these structures yet.

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

Because no controller exists yet, browsing to `http://localhost:8080` may
return the default 404 response.

## Run the tests

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd test
```

The backend requires no database.
