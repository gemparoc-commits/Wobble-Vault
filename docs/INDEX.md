# INDEX — Wobble Vault Docs

This is the map of content for the Wobble Vault project — a shoe-shop ERP (inventory, orders, income, users, permissions). Read this first, then jump to the doc you need.

## 📚 Documentation Map

| Doc | What it covers | Read it when… |
| --- | --- | --- |
| [[OVERVIEW]] | One-paragraph summary, tech stack, features, folder layout, quick-run commands | You are new to the repo and need the big picture. |
| [[ARCHITECTURE]] | Data flow, module layout, DB schema, security model, frontend↔backend wiring | You are about to change code and need to know how pieces connect. |
| [[SETUP]] | Step-by-step run instructions, every env var / config file, troubleshooting table | You need to run the app locally or deploy it. |
| [[ADMIN]] | How auth works, roles/permissions, creating users & admins, DO-NOT warnings | You manage users, permissions, or production credentials. |

## 🧭 Suggested reading order

1. [[INDEX]] (this page)
2. [[OVERVIEW]]
3. [[ARCHITECTURE]]
4. [[ADMIN]] (auth is central to everything here)
5. [[SETUP]] (then actually run it)

## 🔑 Key Facts

### Ports
- Backend, **local profile**: `8081` (override via `PORT` env)
- Backend, **default / prod**: `8080` (override via `PORT` env)
- Frontend dev server (CRA): `3000` (`npm start`)
- Backend localhost discovery probe range: `8080`–`8090`

### URLs
- Backend API (prod): set in `frontend/.env.production` as `REACT_APP_API_URL`
- Health check endpoint: `GET /api/auth/health` (public), also `GET /ping` (public), `GET /actuator/health` (public)
- Swagger/OpenAPI: enabled only outside prod (`/swagger-ui.html`, `/v3/api-docs`), requires `ROLE_ADMIN`; disabled in prod.

### Where credentials / secrets live (locations only)
- `JWT_SECRET` — backend env var.
- `DATABASE_URL` / `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` — backend env vars.
- `APP_BOOTSTRAP_ADMIN_ENABLED`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_PASSWORD` — backend env vars.
- `CORS_ORIGINS` — backend env var; comma-separated allowed origins.
- `REACT_APP_API_URL` — frontend build-time env; `frontend/.env` (empty for dev), `frontend/.env.production`.
- Frontend JWT storage: browser `localStorage` keys `wobble:accessToken` and `wobble:currentUser`.
- Backend auth cookie: `WV_AUTH` (httpOnly).
- CSRF cookie: `XSRF-TOKEN` (not httpOnly, read by axios), submitted as `X-XSRF-TOKEN` header.

### Gotchas (top 5)
1. Schema created by JPA `ddl-auto=update`; Flyway off by default.
2. Non-admin users can be created without email/password — cannot log in.
3. Prod DB falls back to in-memory H2 if `DATABASE_URL` is missing — data vanishes on restart.
4. Order status: ACTIVE | ARCHIVED | CANCELLED; creating deducts stock; cancelling restores stock.
5. Partial payment at creation; extra payments via the payment-update endpoint; editing re-syncs income entry.

## 📎 Related
- [[INDEX]] — start here
- [[OVERVIEW]] — summary, stack, features, quick-run
- [[ARCHITECTURE]] — data flow, schema, security model
- [[SETUP]] — running and deploying
- [[ADMIN]] — users, roles, permissions, DO-NOT warnings
