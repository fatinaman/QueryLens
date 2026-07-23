# QueryLens

QueryLens converts a supported subset of PostgreSQL `CREATE TABLE` statements
into an interactive entity-relationship diagram. It combines a stateless
Spring Boot parser API with a React Flow visualization client.

## Features

- Parse tables, ordered columns, PostgreSQL data types, nullability, primary
  keys, and foreign keys.
- Render draggable table nodes and directional relationship edges.
- Load an example schema, submit with `Ctrl+Enter`, and explore with zoom,
  fit-view, and minimap controls.
- Show structured validation, parser, response-contract, and network errors
  while preserving the last successful diagram.
- Run without a database, authentication, or schema persistence.

Advanced PostgreSQL syntax outside the documented MVP subset may not be
supported. See [the API contract](docs/API.md) for details.

## Architecture

```text
Browser
  |
  | HTTP/JSON
  v
React 19 + TypeScript 6 + React Flow
  |
  | POST /api/schema/parse
  v
Spring Boot 4.1 + Java 21
  |
  v
JSqlParser 5.3 (stateless parsing)
```

The frontend and backend are separately buildable applications. Docker Compose
runs the same two-service architecture; it does not add a database.

## Prerequisites

- Java Development Kit 21
- Node.js 24 LTS and npm 11
- Docker with Compose v2, only for container workflows

Maven is provided through the repository wrapper.

## Local development

Start the backend:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd spring-boot:run
```

Start the frontend in another terminal:

```powershell
Set-Location 'G:\projects for github\QueryLens\frontend'
npm ci
Copy-Item .env.example .env
npm run dev
```

Vite normally serves the frontend at `http://localhost:5173`; the API defaults
to `http://localhost:8080`.

## Environment variables

| Variable | Component | Default | Purpose |
| --- | --- | --- | --- |
| `SERVER_PORT` | Backend | `8080` | HTTP listener port |
| `ALLOWED_ORIGINS` | Backend | `http://localhost:5173` | Comma-separated CORS origins |
| `VITE_API_BASE_URL` | Frontend build | `http://localhost:8080` | Backend base URL embedded by Vite |

Environment files must not contain committed secrets. QueryLens currently
requires no secrets.

## Testing and builds

Backend:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd clean test
```

Frontend:

```powershell
Set-Location 'G:\projects for github\QueryLens\frontend'
npm ci
npm run lint
npm run test
npm run build
```

The frontend suite uses Vitest, React Testing Library, jest-dom, user-event,
and jsdom. Tests cover the API client, editor behavior, diagram transforms,
table rendering, application states, cancellation, and last-result
preservation.

## Docker

Build and start both services:

```powershell
Set-Location 'G:\projects for github\QueryLens'
docker compose config
docker compose build
docker compose up
```

Open `http://localhost:3000`. The backend is exposed at
`http://localhost:8080`. Stop and remove the containers with:

```powershell
docker compose down
```

The backend image uses a multi-stage Java 21 build, runs tests while building,
and runs as a non-root user. The frontend image builds with Node 24 and serves
hashed static assets through nginx with SPA fallback and cache headers.

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on pushes and pull
requests. Independent backend and frontend jobs test and build the code using
Java 21 and Node 24. A dependent Docker job builds both images. The workflow
does not deploy, publish images, or use secrets.

## Troubleshooting

- **Frontend cannot reach the API:** confirm the backend is listening on port
  8080 and `VITE_API_BASE_URL` was set before building or starting Vite.
- **Browser reports CORS errors:** include the exact frontend origin in
  `ALLOWED_ORIGINS`; Compose uses `http://localhost:3000`.
- **Java version errors:** verify `java -version` reports Java 21 and
  `JAVA_HOME` points to that JDK.
- **Node engine errors:** verify `node --version` reports 24.x, remove
  `frontend/node_modules`, and rerun `npm ci`.
- **Port already in use:** stop the conflicting process or change
  `SERVER_PORT` and the matching frontend API URL.
- **Docker cannot connect:** start Docker Desktop or the Docker engine before
  running Compose commands.

## Repository structure

```text
QueryLens/
|-- .github/workflows/ci.yml
|-- backend/                 Spring Boot API and backend container
|-- docs/                    API contract and milestone history
|-- frontend/                React application and nginx container
|-- docker-compose.yml
`-- README.md
```

## Current limitations

QueryLens intentionally has no authentication, persistence, database, AI
analysis, query optimization, or deployment configuration. Unsupported
PostgreSQL features include several advanced DDL constructs documented in
[the backend README](backend/README.md).
