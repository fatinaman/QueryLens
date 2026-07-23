# QueryLens Frontend

The frontend accepts PostgreSQL `CREATE TABLE` statements, calls the QueryLens
backend, and renders the parsed schema as an interactive React Flow diagram.

## Stack

- Node.js 24 LTS and npm 11
- React 19.2.8 and React DOM 19.2.8
- TypeScript 6.0.3
- Vite 8.1.5
- React Flow 12.11.2
- Plain CSS
- Vitest 4.1.10
- React Testing Library 16.3.2
- jest-dom 7.0.0
- user-event 14.6.1
- jsdom 29.1.1

## Structure

```text
src/
|-- api/             Typed fetch client and API tests
|-- components/      Editor, canvas, table node, and component tests
|-- constants/       Built-in SQL example
|-- test/            Shared deterministic setup and fixtures
|-- types/           Backend contract
|-- utils/           Diagram transformation and tests
|-- App.tsx
`-- App.test.tsx
```

## Environment

`VITE_API_BASE_URL` specifies the backend base URL:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Copy `.env.example` to `.env` for a local override. The application removes
trailing slashes and otherwise falls back to `http://localhost:8080`.
Because Vite embeds this value at build time, container or production builds
must supply the URL as a Docker build argument or build environment variable.

## Local development

Use Node.js 24:

```powershell
Set-Location 'G:\projects for github\QueryLens\frontend'
npm ci
npm run dev
```

Run the backend separately on the configured API URL.

## Testing and quality checks

```powershell
npm run lint
npm run test
npm run build
```

For local watch mode:

```powershell
npm run test:watch
```

The deterministic test suite covers:

- successful, structured-error, malformed, invalid-contract, network, and
  aborted API requests;
- editor labels, limits, actions, disabled states, and keyboard submission;
- deterministic node placement and foreign-key edge behavior;
- custom table-node content and handles;
- application empty, loading, success, parser-error, and network states;
- cancellation during unmount;
- and preservation of the last successful diagram after a failed request.

Tests use no snapshots or real timing delays.

## Docker and nginx

Build and run the frontend image:

```powershell
docker build `
  --build-arg VITE_API_BASE_URL=http://localhost:8080 `
  -t querylens-frontend `
  ./frontend
docker run --rm -p 3000:80 querylens-frontend
```

The multi-stage build uses Node 24 and `npm ci`, then serves only the Vite
output from nginx. nginx provides SPA fallback, long-lived immutable caching
for hashed assets, no-cache HTML behavior, and gzip compression.

## Current behavior

- Standard textarea with a 100,000-character limit and `Ctrl+Enter`.
- Built-in example loading without automatic submission.
- Draggable table nodes with textual PK, FK, type, and nullability indicators.
- Deterministic layout and directed foreign-key edges.
- Zoom, fit-view, background grid, and responsive minimap.
- Safe loading, validation, parsing, invalid-response, and network feedback.
- Last successful diagram preservation and request cancellation.

## Troubleshooting

- Verify Node 24 with `node --version` if dependency installation reports an
  engine mismatch.
- Delete `node_modules` and use `npm ci` when the lockfile and installation
  differ.
- Confirm the backend is running and `VITE_API_BASE_URL` is correct when the
  network error panel appears.
- Add the frontend origin to backend `ALLOWED_ORIGINS` when the browser reports
  CORS errors.
- Rebuild the image after changing `VITE_API_BASE_URL`; it is a build-time
  value.

## Limitations

The frontend has no routing, authentication, persistence, deployment
configuration, or offline mode. It visualizes only the schema subset returned
by the backend and does not create edges to referenced tables absent from the
submitted schema.
