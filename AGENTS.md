# AGENTS.md — For AI agents & humans

Wobble Vault: shoe-shop ERP. React 19/CRA frontend + Spring Boot 4.0.6/Java 17 backend. Full docs: see `docs/` (Obsidian). Read [[INDEX]] then [[ARCHITECTURE]] before editing.

## Repo layout
- `backend/backend/` — Spring Boot API (package `com.wobblevault.backend`). Maven wrapper `mvnw.cmd`.
- `frontend/` — React app (web-first; mobile-first styling).
- `docs/` — INDEX, OVERVIEW, ARCHITECTURE, SETUP, ADMIN (wiki-linked).

## Commands
- Backend dev: `cd backend/backend; .\mvnw.cmd spring-boot:run` (port 8081, H2, profile `local`).
- Backend test: `.\mvnw.cmd test`; build: `.\mvnw.cmd clean package` (prod needs `--spring.profiles.active=prod`).
- Frontend dev: `cd frontend; npm start` (port 3000). Test: `npm test`. Build: `npm run build`.

## Rules (strict)
- Facts must come from code, never invent. For behavior/ports/routes, cite the file.
- Never print real secrets or credentials; state only *where* they live (e.g. Render env `JWT_SECRET`).
- `docs/*.md` follow Obsidian style: wiki-links `[[NoteName]]` (no `.md`); every doc ends with `## 📎 Related` linking INDEX + all siblings.
- AGENTS.md stays terse (~40–60 lines). Keep it in sync with the code, not the other way round.
- No source-code comments unless asked.

## Key facts
- Ports: backend default 8080 / local 8081; frontend 3000; API discovery probes 8080–8090.
- Prod URLs: none yet; set `REACT_APP_API_URL` (`.env.production`) and `CORS_ORIGINS` at deployment.
- Auth: JWT 24 h; httpOnly cookie `WV_AUTH` or `Authorization: Bearer`; CSRF cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN`; stateless; 5 failed logins → 15-min lock.
- Authorization: `@PreAuthorize` per endpoint (`hasRole('ADMIN')` / `hasAuthority('<PAGE_NAME>')`). Authorities = ROLE_ + permissions.
- Roles enum: ADMIN, EMPLOYEE, MARKETING, PRODUCTION, SEWING. Permissions (page_name): INVENTORY, ORDERS, SALES, SALES_ARCHIVE, ACCOUNTS (+ derived EMPLOYEES=ADMIN).
- Users: username unique 3–100; ADMIN requires email+password; non-admin email/password nullable; password min 6; salary > 0.
- Data: Hibernate `ddl-auto=update` creates schema; Flyway off by default; schema fresh per instance. All PKs UUID.
- Idempotency: `request_fingerprint` dedup on write endpoints.
- Env secrets live in Render/Supabase deployment env: `JWT_SECRET`, `DATABASE_URL`/`SPRING_DATASOURCE_*`, `APP_BOOTSTRAP_ADMIN_*`, `CORS_ORIGINS`, `SPRING_FLYWAY_ENABLED`.
- Frontend localStorage keys: `wobble:accessToken`, `wobble:currentUser`.
- Mobile-first: card-based lists, full-screen modals <768px, ≥44px touch targets.

## Gotchas
- Dev admin only exists on H2 local profile (auto-seeded when configured).
- Prod without `DATABASE_URL` uses ephemeral in-memory H2 → data loss on restart.
- Frontend keeps 10-min `GET /ping` keep-alive when `REACT_APP_API_URL` set.

## Docs
- [[INDEX]] · [[OVERVIEW]] · [[ARCHITECTURE]] · [[SETUP]] · [[ADMIN]]
