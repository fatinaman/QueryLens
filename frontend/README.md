# QueryLens Frontend

The QueryLens frontend turns PostgreSQL `CREATE TABLE` statements into an
interactive entity-relationship diagram by calling the local Spring Boot API.

## Technology

- React 19.2.8
- TypeScript 6.0.3
- Vite 8.1.5
- React Flow 12.11.2 through `@xyflow/react`
- Plain CSS

## Structure

```text
src/
├── api/           Backend API client
├── components/    SQL editor, diagram canvas, and custom table nodes
├── constants/     Built-in example schema
├── types/         Backend API contract
├── utils/         Deterministic React Flow node and edge transformation
├── App.tsx
├── App.css
└── index.css
```

## Environment

Copy `.env.example` to `.env` only when a local override is needed:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

The frontend falls back to `http://localhost:8080` and removes trailing
slashes before constructing API requests.

## Install and run

Use Node.js 24 LTS:

```powershell
Set-Location 'G:\projects for github\QueryLens\frontend'
npm install
npm run dev
```

The Vite output prints the exact local URL, normally
`http://localhost:5173`.

The backend must also be running:

```powershell
Set-Location 'G:\projects for github\QueryLens\backend'
.\mvnw.cmd spring-boot:run
```

## Quality checks

```powershell
npm run lint
npm run build
```

The production output is written to `dist`.

## Current behavior

- Edit up to 100,000 characters of SQL in a standard textarea.
- Load the built-in three-table example without submitting it.
- Generate an interactive diagram with draggable table nodes.
- Inspect primary-key, foreign-key, data-type, and nullability indicators.
- Follow directed foreign-key edges between included tables.
- Zoom, pan, fit the diagram, and use the MiniMap on wider screens.
- Keep the latest successful diagram visible when a later request fails.
- Display safe validation, parser, invalid-response, and network errors.
- Clear the SQL, diagram, errors, and active request together.

References to tables outside the submitted schema keep their FK indicators but
do not create broken edges.

## Limitations

Advanced PostgreSQL features outside QueryLens's documented MVP subset may not
be supported. The frontend has no automated test suite yet, is not deployed,
and has no persistence or authentication.
