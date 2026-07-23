# QueryLens

QueryLens turns a supported subset of PostgreSQL `CREATE TABLE` statements
into an interactive entity-relationship diagram. Paste a schema, send it to
the stateless Spring Boot parser, and explore the resulting tables and
foreign-key relationships in the browser.

## MVP features

- PostgreSQL schema input with an included example and `Ctrl+Enter` shortcut.
- Parsing for tables, ordered columns, data types, nullability, primary keys,
  and foreign keys.
- Draggable React Flow table nodes, directional relationship edges, zoom
  controls, minimap, and deterministic initial layout.
- Structured validation, parsing, unexpected-response, and network feedback.
- The last successful diagram remains visible if a later request fails.

Advanced PostgreSQL syntax outside the documented MVP subset may not be
supported. See [the API documentation](docs/API.md) for the exact contract.

## Technology stack

### Frontend

- Node.js 24 and npm 11
- React 19 with TypeScript 6
- Vite 8
- React Flow (`@xyflow/react` 12)
- Plain CSS

### Backend

- Java 21
- Spring Boot 4.1.0
- Maven Wrapper 3.3.4 (Maven 3.9.16)
- JSqlParser 5.3
- JUnit

The MVP uses one stateless Spring Boot service. It has no database,
authentication, or schema persistence.

## Run locally

### Backend

From `backend` in PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`.

### Frontend

From `frontend` in a second PowerShell window:

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

Open the local URL printed by Vite. `VITE_API_BASE_URL` defaults to
`http://localhost:8080` when it is unset.

Build and lint the frontend with:

```powershell
npm run lint
npm run build
```

Run the backend test suite with:

```powershell
.\mvnw.cmd clean test
```

## Repository structure

```text
QueryLens/
|-- backend/             Spring Boot parser and REST API
|-- docs/                API contract and project history
|-- frontend/            React and Vite application
|-- .github/workflows/   Reserved for a later CI milestone
|-- .env.example         Repository-level environment reference
`-- README.md
```

## Project status

Milestones 1–6 are complete. Milestones 7–11 are implemented locally and have
passed lint, production-build, backend-test, server-startup, and live API
checks. Their final manual browser verification remains pending because no
interactive browser was connected to this development session. No frontend
test framework, Docker configuration, CI workflow, deployment, or hosting
configuration has been added.

See [Project State](docs/PROJECT_STATE.md) for technical decisions, milestone
history, verification results, and the next planned work.
