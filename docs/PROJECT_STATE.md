# QueryLens Project State

## Current milestone

**Milestone 3:** Request and response DTOs

## Current status

**In progress**

## Established technical decisions

- Use one monorepo-style Git repository containing `backend` and `frontend`
  folders.
- Use Java 21.
- Use Spring Boot 4.1.0.
- Use Maven with Maven Wrapper 3.3.4 (Maven 3.9.16).
- Use React with TypeScript.
- Use Vite.
- Use React Flow through the `@xyflow/react` package.
- Use plain CSS for the MVP.
- Do not use a database.
- Do not add authentication.
- Do not persist SQL schemas.
- Build one Spring Boot service, not microservices.
- Deploy the frontend and backend separately.
- Include Spring Web, Spring Boot Validation, and Spring Boot Test in the
  backend.
- Do not include database dependencies.
- Use Java records for immutable API DTOs.
- Limit schema parsing request SQL to 100,000 characters.
- Defensively copy DTO collection fields into immutable lists, converting null
  collections to empty lists.

## Repository structure

```text
QueryLens/
├── .github/
│   └── workflows/
├── backend/
│   ├── .mvn/wrapper/
│   ├── src/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── README.md
├── docs/
│   └── PROJECT_STATE.md
├── frontend/
├── .editorconfig
├── .env.example
├── .gitattributes
├── .gitignore
└── README.md
```

The otherwise empty `frontend` and `.github/workflows` directories contain
`.gitkeep` files so Git can track them. The backend now contains the Spring Boot
project skeleton.

## Milestone history

| Milestone | Status | Outcome |
| --- | --- | --- |
| Milestone 1 | Complete | Established repository structure, prerequisites documentation, and initial configuration. |
| Milestone 2 | Complete | Established a runnable Java 21 and Spring Boot 4.1.0 backend with Maven Wrapper and a context test. |
| Milestone 3 | In progress | Define immutable request, success-response, and error-response DTOs for the future schema parsing API. |

## Outcome of Milestone 1

- Initialized the Git repository with `main` as the default branch.
- Created tracked placeholders for the backend, frontend, and workflow
  directories.
- Added repository-wide editor, line-ending, ignore, and environment-variable
  examples.
- Documented the planned stack, prerequisites, architecture constraints, and
  current project state.
- Left application generation and dependency installation for later
  milestones.

## Outcome of Milestone 2

- Added a Java 21 Spring Boot 4.1.0 Maven project under `backend`.
- Included Maven Wrapper 3.3.4, configured to use Maven 3.9.16.
- Added only Spring Web, Spring Boot Validation, and Spring Boot Test.
- Added the standard application entry point and a context-loading test.
- Verified compilation, tests, and application startup without a database.

## Planned outcome of Milestone 3

- Add `SchemaParseRequest` with blank-input validation and a 100,000-character
  SQL limit.
- Add `ColumnDto`, `ForeignKeyDto`, `TableDto`, and `SchemaParseResponse` for
  successful responses.
- Add `ApiErrorResponse` for error responses.
- Use Java records and immutable defensive copies for collection fields.
- Verify validation, immutability, JSON property names, serialization, and
  deserialization with focused DTO tests.
- Define the contract only; no endpoint or SQL parser exists yet.

## Next milestone

**Milestone 4: SQL parser implementation**

The next milestone will implement SQL schema parsing. It has not started.
