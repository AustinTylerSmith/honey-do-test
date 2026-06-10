# Honey Do (Test)

## Restrictions

- **NEVER**: commit
- **NEVER**: stage
- **NEVER**: pull
- **NEVER**: push

## Project Overview

Honey Do is a full-stack monorepo for managing shared household task lists. It uses:

- **Backend**: Spring Boot 3.5.0 (Java 22) with Spring JDBC (`JdbcTemplate`) + Flyway migrations, backed by a SQLite database
- **Frontend**: Vue 3 with TypeScript and Vite
- **Testing**: JUnit 5 (backend); frontend testing is not yet set up

The frontend is a separate Vite dev server (port 3000) that proxies `/api/*` requests to the backend (port 8080). It does not currently build into the backend's static resources.

## Build & Test Commands

### Backend (Java/Gradle)

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Build without tests
./gradlew build -x test

# Run the application (http://localhost:8080, endpoints under /api)
./gradlew bootRun
```

### Frontend (Node/npm)

From the `frontend/` directory:

```bash
# Install dependencies
npm install

# Development server (port 3000, proxies /api to localhost:8080)
npm run dev

# Type-check
npm run type-check

# Build for production
npm run build

# Preview a production build
npm run preview
```

## Architecture

### Backend structure (`src/main/java/com/honeydo/`)
- **`entity/`**: Plain POJOs representing DB rows, `*Entity` suffix (no JPA — mapped manually via `JdbcTemplate` `RowMapper`s)
- **`dao/`**: `JdbcTemplate`-based data access, `*DAO` suffix, annotated `@Repository`
- **`dto/`**: Request/response POJOs, `Request`/`Response` suffix
- **`service/`**: Business logic, `@Service`, constructor-injected DAOs
- **`controller/`**: `@RestController` classes, REST endpoints
- **`security/`**: JWT + Spring Security components (`JwtService`, filters, entry points, `UserDetailsService`)
- **`config/`**: Spring `@Configuration` classes (e.g. `SecurityConfig`)
- **`exception/`**: Custom runtime exceptions + a shared `@RestControllerAdvice` (`GlobalExceptionHandler`)

### Database
- SQLite file at `./honeydo.db`, committed at the repo root
- Flyway migrations in `src/main/resources/db/migration/`, named `V###__description.sql`
- Local-only config/secrets (e.g. a non-default `jwt.secret`) go in `application-local.properties` (gitignored, optionally imported)

### Frontend structure (`frontend/src/`)
- Currently a Vite/Vue scaffold — `vue-router`, Pinia, and a UI component library are not yet installed
- `@` is aliased to `frontend/src`

## Key Conventions

### Naming
- **Entities**: `*Entity` suffix (e.g. `UserEntity`, `TaskEntity`) — plain POJOs, not JPA
- **DAOs**: `*DAO` suffix (e.g. `UserDAO`, `TaskDAO`), `JdbcTemplate`-based, `@Repository`
- **DTOs**: `Request`/`Response` suffix in `dto/`
- **Services**: one concrete `@Service` per domain, constructor injection
- **Controllers**: `@RestController`, route paths exclude the `/api` context-path prefix (e.g. `@RequestMapping("/tasks")` is exposed as `/api/tasks`)
- **Exceptions**: custom `RuntimeException`s in `exception/`, mapped to HTTP responses via `GlobalExceptionHandler`

### Migrations
- Flyway, `src/main/resources/db/migration/V###__description.sql`
- SQLite syntax (`INTEGER PRIMARY KEY AUTOINCREMENT`, `TEXT` timestamps via `datetime('now')`)
- No explicit `BEGIN`/`COMMIT` — Flyway manages transactions
- SQLite is supported directly by `flyway-core` (Flyway 10) — no extra database module needed

### Security
- Spring Security 6 (lambda `SecurityFilterChain`) + JWT (`io.jsonwebtoken:jjwt`) + BCrypt password hashing
- `/auth/**` is public; every other endpoint requires a valid `Authorization: Bearer <jwt>` header
- `jwt.secret` / `jwt.expiration-ms` live in `application.properties`; override `jwt.secret` via `application-local.properties` for any non-local environment

### Validation
- `jakarta.validation` annotations on request DTOs (`@NotBlank`, `@Email`, `@Size`) via `spring-boot-starter-validation`
- Validation failures are translated to `400` responses by `GlobalExceptionHandler`

## Service-Layer Error Handling

DAO calls in service classes should be wrapped in try-catch using these conventions:

- **Catch type**: `org.springframework.dao.DataAccessException` — the base class for `JdbcTemplate` errors. Do **not** catch `Exception`/`RuntimeException`, as that would swallow intentional exceptions (e.g. `EmailAlreadyExistsException`, `UsernameNotFoundException`).
- **Logger**: `private static final Logger log = LoggerFactory.getLogger(ClassName.class)` (SLF4J) per class.
- **Log level**: `log.error(...)` with a descriptive message including the relevant id/key, followed by the exception.
- **Rethrow**: always rethrow so the controller layer/exception handler is unaffected.
- For methods with multiple DAO calls, use a separate try-catch per call with a distinct log message.

## Java Unit Testing Patterns

- Test packages mirror `src/main/java` structure under `src/test/java`
- Controller/integration tests: `@SpringBootTest` + `@AutoConfigureMockMvc` + `MockMvc`, `ObjectMapper` for (de)serialization
- Pure unit tests (no Spring context) for framework-free classes (e.g. `JwtServiceTest`)
- `src/test/resources/application.properties` overrides the datasource to in-memory SQLite (`jdbc:sqlite::memory:`, `spring.datasource.hikari.maximum-pool-size=1`) so Flyway and `JdbcTemplate` share a single connection/schema for the test context
- Test naming: `ClassNameTest`, descriptive method names (e.g. `loginWithWrongPasswordReturnsUnauthorized`)
- Use AssertJ assertions (`assertThat(...)`)
- Run with `./gradlew test`

## Common Tasks

**Add a new backend feature/endpoint**:
1. Add a Flyway migration in `src/main/resources/db/migration/`
2. Create `*Entity` POJO in `entity/`
3. Create `*DAO` in `dao/` (`JdbcTemplate` + `RowMapper`)
4. Create request/response DTOs in `dto/`
5. Create `@Service` in `service/`
6. Create `@RestController` in `controller/`
7. Add `GlobalExceptionHandler` cases for new exception types if needed
8. Add tests under `src/test/java` mirroring the package structure

**Run backend in dev**: `./gradlew bootRun` (port 8080, all endpoints under `/api`)
**Run frontend in dev**: `cd frontend && npm run dev` (port 3000)
