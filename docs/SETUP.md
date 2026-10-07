# SETUP — Wobble Vault

## Prerequisites
- Java 17 (matching backend)
- Node.js 18+ (tested with v22), npm
- Maven wrapper included (`mvnw.cmd`)

## Local development

### Backend
1. `cd backend/backend`
2. `.\mvnw.cmd spring-boot:run`
3. Runs on http://localhost:8081 (H2 in-memory/local profile). Profile `local` by default unless `SPRING_PROFILES_ACTIVE` set.

### Frontend
1. `cd frontend`
2. `npm install` (first time)
3. `npm start` → http://localhost:3000
4. On localhost without `REACT_APP_API_URL`, API discovery probes 8080–8090.

## Environment variables

### Backend
- `SPRING_PROFILES_ACTIVE` (default: `local`)
- `PORT` (default 8080; local 8081)
- `JWT_SECRET` (required in prod; dev defaults provided)
- `DATABASE_URL`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (Postgres prod)
- `SPRING_JPA_HIBERNATE_DDL_AUTO` (default `update`)
- `SPRING_FLYWAY_ENABLED` (default `false`)
- `CORS_ORIGINS` (comma-separated)
- `APP_BOOTSTRAP_ADMIN_ENABLED`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_PASSWORD`

### Frontend
- `REACT_APP_API_URL` (build-time; empty for dev discovery; set in `.env.production` for prod)

## Build & test
- Backend test: `.\mvnw.cmd test` (from `backend/backend`)
- Backend build: `.\mvnw.cmd clean package` (prod add `--spring.profiles.active=prod`)
- Frontend test: `npm test -- --watchAll=false` (from `frontend`)
- Frontend build: `npm run build`

## Deployment notes
- Prod backend: set `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET`, `DATABASE_URL` (or datasource vars). Without `DATABASE_URL`, uses ephemeral in-memory H2 (data loss on restart).
- CORS must include frontend origin in `CORS_ORIGINS`.
- CSRF cookies: prod typically `Secure=true`, `SameSite=None`; local `Secure=false`, `SameSite=Lax`.

## Troubleshooting
| Issue | Cause | Fix |
| --- | --- | --- |
| CORS errors | Origin not in `CORS_ORIGINS` | Add frontend origin to backend env var |
| Auth loop on login | CSRF not set / cookie blocked | Ensure `withCredentials` true; check SameSite/Secure for cross-site |
| API not discovered on localhost | Backend not running on 8080–8090 | Start backend on expected port or set `REACT_APP_API_URL` |
| Data lost after restart (prod) | No `DATABASE_URL` set | Configure Postgres connection string |

## 📎 Related
- [[INDEX]] — map of content
- [[OVERVIEW]] — summary, stack, quick-run
- [[ARCHITECTURE]] — data flow, schema, security model
- [[SETUP]] — this page
- [[ADMIN]] — users, roles, permissions
