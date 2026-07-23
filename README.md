# QueryLens

QueryLens is a planned developer tool for turning PostgreSQL `CREATE TABLE`
statements into an interactive entity-relationship diagram.

## Planned MVP features

- Paste PostgreSQL `CREATE TABLE` statements.
- Parse tables, columns, primary keys, and foreign-key relationships.
- Display the resulting schema as an interactive ER diagram.
- Provide useful validation feedback for unsupported or invalid input.

These features are planned only; none of the application features currently
works.

## Planned technology stack

### Frontend

- React with TypeScript
- Vite
- React Flow through the `@xyflow/react` package
- Plain CSS for the MVP

### Backend

- Java 21
- Spring Boot 4.1.x
- Maven, with the Maven Wrapper to be added during backend setup
- REST API
- JUnit

The MVP will use one Spring Boot service. It will have no database,
authentication, or SQL schema persistence.

## Deployment plan

The frontend and backend will be deployed separately. The frontend is planned
for Vercel or Cloudflare Pages, and the backend for Render or another suitable
free hosting service.

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

## Local prerequisites

- Git
- Java Development Kit (JDK) 21
- Node.js and npm for future frontend development

Version-specific Node.js and npm requirements will be established when the
frontend is initialized.

The backend includes the Maven Wrapper, so a global Maven installation is not
required. See the [backend README](backend/README.md) for PowerShell run and
test commands.

## Current project status

Milestones 1 and 2 are complete. Milestone 3 is in progress: the backend now
defines an immutable API DTO contract for future schema parsing requests,
successful responses, and error responses. The schema parsing endpoint and SQL
parser are not implemented. No React or Vite application has been generated,
and no schema parsing functionality exists.

See [Project State](docs/PROJECT_STATE.md) for technical decisions, milestone
history, and the next planned step.
