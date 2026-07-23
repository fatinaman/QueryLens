# QueryLens Deployment Guide

This guide prepares QueryLens for free public hosting with:

- GitHub as the source repository and provider-native deployment trigger
- Render Web Service for the Spring Boot backend
- Cloudflare Pages for the static React and Vite frontend

The repository contains no provider credentials, deployment tokens, database,
or application secrets. Complete the provider dashboard steps manually.

## Deployment status

```text
Live demo: Not deployed yet
Public API: Not deployed yet
```

Do not replace these values until both public services have been deployed and
verified.

## Required deployment order

1. Push the repository to GitHub.
2. Deploy the backend on Render.
3. Copy the Render backend HTTPS URL.
4. Create the Cloudflare Pages project from the GitHub repository.
5. Set `VITE_API_BASE_URL` to the Render URL.
6. Deploy the frontend.
7. Copy the Cloudflare Pages production URL.
8. Return to the Render service.
9. Set `ALLOWED_ORIGINS` to the exact Cloudflare Pages origin.
10. Redeploy or restart the Render service.
11. Complete the production QA checklist.

Two provider passes are necessary: Cloudflare needs the Render URL at build
time, while Render CORS needs the final Cloudflare origin.

## Render backend

Render supplies a `PORT` environment variable. QueryLens resolves its port in
this order:

1. `PORT`
2. `SERVER_PORT`
3. `8080`

Do not manually set `PORT` in normal Render configuration. The Docker image
still exposes port 8080 as documentation, and local Docker Compose continues
to use port 8080.

### Method 1: Render Blueprint

1. Open the Render dashboard.
2. Create a Blueprint and connect the QueryLens GitHub repository.
3. Select the repository and allow Render to detect the root `render.yaml`.
4. Review the proposed `querylens-api` Docker Web Service.
5. When prompted for `ALLOWED_ORIGINS`, enter the exact Cloudflare production
   origin if it is already known:

   ```text
   https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>
   ```

   If the dashboard requires a value before Cloudflare exists, use a clearly
   temporary value that is not a wildcard, deploy the backend, and replace it
   with the exact Cloudflare origin after step 7 of the deployment order.

6. Create the Blueprint resources and wait for the backend build to finish.
7. Copy the public HTTPS URL. It will resemble:

   ```text
   https://<YOUR-RENDER-SERVICE-NAME>.onrender.com
   ```

Dashboard labels can change, but the service should remain a Docker Web
Service on the free instance type when that option is available.

### Method 2: Manual Docker Web Service

Create a new service with these settings:

| Setting | Value |
| --- | --- |
| Service type | Web Service |
| Runtime | Docker |
| Repository | Your QueryLens GitHub repository |
| Branch | `main` |
| Root directory | `backend` |
| Dockerfile | `Dockerfile` |
| Instance type | Free, if currently offered |

Add this runtime environment variable:

```text
ALLOWED_ORIGINS=https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>
```

For a temporary local-plus-production transition, comma-separated origins are
supported:

```text
ALLOWED_ORIGINS=https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>,http://localhost:5173
```

Do not use `*`, enable credentials, or add a manual `PORT` value. Render
provides `PORT`.

### Render automatic deployments

The Blueprint enables deployment after commits to the linked production
branch. A manually created service can enable the equivalent auto-deploy
setting in its dashboard. Render provider builds and GitHub Actions are
separate systems; successful CI does not prove that Render deployed
successfully. Inspect the Render build and runtime logs after the first
release.

## Cloudflare Pages frontend

Create a Pages project using Cloudflare's Git integration. Do not create a
Worker, Pages Function, Wrangler configuration, or direct-upload project.

Use these build settings:

| Setting | Value |
| --- | --- |
| Production branch | `main` |
| Root directory | `frontend` |
| Build command | `npm ci && npm run build` |
| Build output directory | `dist` |

Add this production build environment variable:

```text
VITE_API_BASE_URL=https://<YOUR-RENDER-BACKEND-DOMAIN>
```

QueryLens requires Node.js 24. If the Pages build image does not select it
from the project automatically, add:

```text
NODE_VERSION=24
```

`VITE_API_BASE_URL` is a build-time value embedded in browser JavaScript.
Changing it requires a new frontend deployment. Every `VITE_` variable is
visible to browser users, so never store secrets, credentials, or private
tokens in it.

After the first successful deployment, copy the exact Pages production origin
and update Render:

```text
ALLOWED_ORIGINS=https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>
```

Cloudflare Pages can automatically rebuild the production branch after GitHub
pushes. Provider deployment builds remain separate from GitHub Actions CI.
Inspect the Cloudflare build log after the first release.

## GitHub and CI

Push the reviewed deployment-preparation commit to GitHub. The existing CI
workflow verifies:

- backend tests on Java 21
- frontend installation, lint, tests, and build on Node 24
- backend and frontend Docker image builds

Where branch protection is configured, require CI checks before merging to
`main`. Do not add provider tokens or deployment jobs; Render and Cloudflare
use their own Git integrations.

## Production backend verification

Set the public backend URL in PowerShell:

```powershell
$backendUrl = 'https://<YOUR-RENDER-BACKEND-DOMAIN>'
```

Create a temporary valid request:

```powershell
@'
{
  "sql": "CREATE TABLE users (id BIGSERIAL PRIMARY KEY);"
}
'@ | Set-Content -Encoding utf8 querylens-valid.json

curl.exe -i `
  -X POST `
  -H "Content-Type: application/json" `
  --data-binary "@querylens-valid.json" `
  "$backendUrl/api/schema/parse"
```

Verify HTTP 200 and a JSON `tables` array.

Create an invalid request:

```powershell
@'
{
  "sql": "DROP TABLE users;"
}
'@ | Set-Content -Encoding utf8 querylens-invalid.json

curl.exe -i `
  -X POST `
  -H "Content-Type: application/json" `
  --data-binary "@querylens-invalid.json" `
  "$backendUrl/api/schema/parse"

Remove-Item querylens-valid.json, querylens-invalid.json
```

Verify HTTP 400, the structured `message` and `errors` fields, and the absence
of stack traces or internal exception details. QueryLens performs no database
activity.

## Production CORS verification

Set both public origins:

```powershell
$backendUrl = 'https://<YOUR-RENDER-BACKEND-DOMAIN>'
$frontendOrigin = 'https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>'
```

Check the configured origin:

```powershell
curl.exe -i `
  -X OPTIONS `
  -H "Origin: $frontendOrigin" `
  -H "Access-Control-Request-Method: POST" `
  -H "Access-Control-Request-Headers: Content-Type" `
  "$backendUrl/api/schema/parse"
```

Verify:

```text
Access-Control-Allow-Origin: https://<YOUR-CLOUDFLARE-PAGES-DOMAIN>
```

Check an unconfigured origin:

```powershell
curl.exe -i `
  -X OPTIONS `
  -H "Origin: https://unconfigured.example" `
  -H "Access-Control-Request-Method: POST" `
  "$backendUrl/api/schema/parse"
```

The arbitrary origin must not receive an `Access-Control-Allow-Origin`
response header.

## Production frontend checklist

- [ ] The Pages site loads over HTTPS.
- [ ] **Load Example** fills the editor without submitting.
- [ ] **Generate Diagram** renders `users`, `projects`, and `tasks`.
- [ ] PK and FK text indicators appear.
- [ ] The three expected relationships appear:
  - [ ] `projects.owner_id` to `users.id`
  - [ ] `tasks.project_id` to `projects.id`
  - [ ] `tasks.assigned_to` to `users.id`
- [ ] Table dragging, zoom, fit-view, and minimap work.
- [ ] Invalid SQL displays a useful structured error.
- [ ] The last successful diagram remains visible after an error.
- [ ] The browser console contains no CORS or mixed-content errors.
- [ ] Desktop and narrow responsive layouts remain usable.
- [ ] Refreshing the Pages URL still serves the application.

## Free-tier behavior

Free hosting availability and limits can change. A free Render service may
sleep, cold-start slowly, or be temporarily unavailable. QueryLens already
keeps the editor visible and displays understandable network guidance while
the backend wakes up. Do not add aggressive automatic retries.

Cloudflare Pages and Render can impose build-minute, bandwidth, usage, and
resource limits. Review the provider dashboards and current free-tier terms
before release.

## Release completion

Public deployment is complete only after:

- both HTTPS URLs are known,
- Render CORS contains the exact Pages origin,
- both provider deployment logs are clean,
- backend, CORS, and frontend production checks pass,
- and the final URLs are added to the README.
