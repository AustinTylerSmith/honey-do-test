# Honey Do (Test)

Monorepo containing the Honey Do application:

- **Backend** — Spring Boot (Gradle, Java 22) at the repository root (`build.gradle`, `src/`)
- **Frontend** — Vue 3 + TypeScript + Vite at [`frontend/`](./frontend)

## Prerequisites

- Java 22 (`java -version` should report `22.x`), with `JAVA_HOME` pointing at it
- Node.js and npm

## Backend

```
./gradlew bootRun
```

Starts the Spring Boot application on **http://localhost:8080**, with all
endpoints served under the `/api` base path (e.g. `/api/auth/login`). On
startup it connects to the SQLite database file committed at `./honeydo.db`.

### Local configuration

Backend configuration lives in `src/main/resources/application.properties`.
For local-only settings that should never be committed (e.g. SMTP
credentials, secrets), create an `application-local.properties` file at the
repository root — it's gitignored and automatically loaded if present.

## Frontend

```
cd frontend
npm install
npm run dev
```

Starts the Vite dev server on **http://localhost:3000**. Requests to `/api/*`
are proxied to the backend at `http://localhost:8080`.
