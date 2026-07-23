# QueryLens Project State

## Current milestone

**Milestones 12–13:** Frontend automated tests, production containers, Docker
Compose, and continuous integration

## Current status

**Implementation complete; Docker runtime verification pending where Docker is
unavailable**

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
- Require Node.js 24 and npm 11 for frontend development.
- Use React 19, TypeScript 6, Vite 8, and `@xyflow/react` 12.
- Use Vitest 4 with React Testing Library, jest-dom, user-event, and jsdom for
  behavior-focused frontend tests.
- Avoid snapshot tests, real timers, and implementation-detail assertions.
- Build the backend in a multi-stage Java 21 image and run it as a non-root
  user.
- Build the frontend with Node 24 and serve immutable static assets through
  nginx with SPA fallback.
- Run the two applications through Docker Compose without a database.
- Run Java 21 backend checks, Node 24 frontend checks, and both image builds in
  GitHub Actions without publishing or deployment.
- Keep the frontend API base URL configurable through `VITE_API_BASE_URL`,
  falling back to `http://localhost:8080`.
- Validate success and error response bodies at the frontend network boundary.
- Use a deterministic three-column grid for initial table placement.
- Draw foreign-key edges from referencing tables to referenced tables, and
  safely skip relationships whose target table is absent.
- Preserve the last successful diagram when a later parse request fails.
- Abort superseded parse requests and ignore stale responses.
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
- Use JSqlParser 5.3 as the only SQL parsing library.
- Define parsing through `SchemaParser`, implemented by the Spring component
  `JSqlSchemaParser`.
- Translate parser and structural failures into `SchemaParsingException`.
- Support inline, table-level, named, and composite primary and foreign keys.
- Reject unsupported statement types instead of silently ignoring them.
- Expose schema parsing through stateless `POST /api/schema/parse`.
- Delegate HTTP requests through `SchemaController` and `SchemaService` to
  `SchemaParser`.
- Return structured `ApiErrorResponse` bodies from
  `GlobalExceptionHandler`.
- Configure comma-separated CORS origins through `ALLOWED_ORIGINS`, scoped to
  `/api/**`, without credentials.
- Continue to require no database.

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

The `frontend` directory contains the React application and nginx container.
The backend has its own Java container. `.github/workflows/ci.yml` verifies
both applications and images.

## Milestone history

| Milestone | Status | Outcome |
| --- | --- | --- |
| Milestone 1 | Complete | Established repository structure, prerequisites documentation, and initial configuration. |
| Milestone 2 | Complete | Established a runnable Java 21 and Spring Boot 4.1.0 backend with Maven Wrapper and a context test. |
| Milestone 3 | Complete | Defined immutable request, success-response, and error-response DTOs for the future schema parsing API. |
| Milestone 4 | Complete | Implemented the PostgreSQL `CREATE TABLE` parser for the documented MVP subset. |
| Milestone 5 | Complete | Added the schema service, REST endpoint, structured exception handling, configurable CORS, and API documentation. |
| Milestone 6 | Complete | Added service, controller, integration, CORS, and error-contract tests; all 47 backend tests pass. |
| Milestone 7 | Complete | Initialized a React 19 and TypeScript frontend with Vite 8, Node 24 tooling, and React Flow. |
| Milestone 8 | Complete | Added the accessible SQL editor, example schema, clear action, character limit, and keyboard submission. |
| Milestone 9 | Complete | Added custom table nodes, foreign-key edges, deterministic layout, controls, minimap, dragging, and empty state. |
| Milestone 10 | Complete | Integrated the parsing API with cancellation, loading state, runtime response validation, and safe error feedback. |
| Milestone 11 | Complete | Added the responsive split layout, mobile stacking, restrained visual styling, and frontend documentation. |
| Milestone 12 | Complete | Added deterministic behavior-focused frontend coverage for the network, editor, diagram, node, and application layers. |
| Milestone 13 | Implemented; Docker runtime verification pending | Added production containers, Docker Compose, and a no-deployment GitHub Actions pipeline. |

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

## Outcome of Milestone 3

- Added `SchemaParseRequest` with blank-input validation and a 100,000-character
  SQL limit.
- Added `ColumnDto`, `ForeignKeyDto`, `TableDto`, and `SchemaParseResponse` for
  successful responses.
- Added `ApiErrorResponse` for error responses.
- Used Java records and immutable defensive copies for collection fields.
- Verified validation, immutability, JSON property names, serialization, and
  deserialization with focused DTO tests.
- Defined the DTO contract without exposing an endpoint.

## Outcome of Milestone 4

- Added JSqlParser 5.3 as the only new production dependency.
- Added `SchemaParser`, `JSqlSchemaParser`, and `SchemaParsingException`.
- Parsed ordered tables, columns, PostgreSQL-style data types, nullability,
  primary keys, and foreign keys from supported `CREATE TABLE` statements.
- Supported inline, table-level, named, and composite key forms, including
  schema-qualified and forward foreign-key references.
- Rejected non-`CREATE TABLE` statements and malformed input with application
  exceptions.
- Rejected duplicate table and column names, unknown primary-key or foreign-key
  source columns, and mismatched composite foreign keys.
- Verified the parser with direct unit tests and kept the existing DTO and Spring
  context tests passing.
- Kept the parser internal until Milestone 5.

## Outcome of Milestone 5

- Added `SchemaService` to delegate SQL strings to `SchemaParser`.
- Added `SchemaController` with stateless
  `POST /api/schema/parse`, consuming and producing JSON.
- Added Jakarta Bean Validation at the request boundary.
- Added `GlobalExceptionHandler` for parsing errors, validation errors,
  malformed JSON, and safe HTTP 500 responses.
- Preserved normal Spring HTTP 405 and 415 behavior.
- Added `CorsConfig` for comma-separated allowed origins, `POST` and `OPTIONS`,
  the `Content-Type` header, no credentials, and a one-hour preflight cache.
- Added `docs/API.md` with the complete API contract and PowerShell examples.
- Manually verified HTTP 200 success and structured HTTP 400 parsing and
  validation errors.

## Outcome of Milestone 6

- Added plain unit tests for service delegation and exception propagation.
- Added focused controller, validation, content-type, method, and error-contract
  tests.
- Added real-parser endpoint integration tests using the full Spring context.
- Added CORS tests for configured and unconfigured origins and `/api/**`
  scoping.
- Verified all 47 tests on Java 21 with zero failures, errors, or skipped tests.
- Verified application startup, endpoint behavior, shutdown, and absence of
  database activity.
- Added no production or testing dependencies.

## Outcome of Milestones 7–11

- Scaffolded the frontend with Node.js 24.18.0 and npm 11.16.0.
- Installed React 19.2.8, React DOM 19.2.8, TypeScript 6.0.3, Vite 8.1.5,
  and `@xyflow/react` 12.11.2.
- Initially added no frontend test framework; Milestone 12 later introduced the
  dedicated test toolchain.
- Added typed API DTOs and a fetch client with a configurable base URL,
  request cancellation, structured backend errors, network guidance, and
  complete runtime response validation.
- Added an example PostgreSQL schema and an accessible editor with a 100,000
  character limit, live count, disabled/loading states, and `Ctrl+Enter`.
- Added custom draggable table nodes with ordered columns and visible PK, FK,
  type, and nullable indicators.
- Added deterministic grid placement, directional smooth-step foreign-key
  edges, fit-to-view behavior, zoom controls, a minimap, and a useful empty
  state.
- Preserved the last successful diagram on subsequent errors and safely
  ignored stale or aborted requests.
- Added a desktop split workspace and stacked narrow-screen layout, including
  a hidden minimap where horizontal space is limited.
- Verified frontend lint and the production build with Node.js 24.
- Re-ran all 47 backend tests on Java 21 with zero failures, errors, or skips.
- Started both local applications and verified their health, API integration,
  and browser behavior.
- Initially added no Docker, CI, deployment, or hosting configuration.

## Outcome of Milestone 12

- Added Vitest 4, React Testing Library, jest-dom, user-event, and jsdom.
- Added strict TypeScript-aware test configuration and deterministic shared
  fixtures.
- Covered API request construction, response validation, structured backend
  failures, malformed responses, network errors, and abort propagation.
- Covered editor accessibility, limits, actions, loading behavior, and
  keyboard submission.
- Covered deterministic grid placement, directed foreign-key edges, missing
  referenced tables, unique relationships, and punctuation-safe IDs.
- Covered custom node column ordering, textual PK/FK indicators, nullability,
  and connection handles.
- Covered application empty, loading, success, structured-error, network,
  cancellation, clear, and last-successful-diagram behavior.
- Added no snapshots, arbitrary waits, or production behavior changes.
- Verified 31 frontend tests across 6 files with zero failures.

## Outcome of Milestone 13

- Added a multi-stage Java 21 backend image that runs Maven tests during build
  and executes as a dedicated non-root runtime user.
- Added a multi-stage Node 24 frontend image and nginx configuration with SPA
  fallback, gzip, immutable asset caching, and no-cache HTML.
- Added focused Docker ignore files to reduce build contexts.
- Added Docker Compose for the backend on port 8080 and frontend on port 3000,
  including the correct CORS and frontend API settings.
- Added GitHub Actions jobs for Java 21 backend tests, Node 24 install/lint/test/
  build checks, and dependent backend/frontend image builds.
- Added no deployment, registry publication, secrets, database, or cloud
  configuration.
- Docker syntax and build intent were inspected locally; runtime build and
  Compose startup remain pending on a machine with Docker.

## Next milestone

**Milestone 14 — deployment**

Deployment has not started and remains outside the current engineering scope.
