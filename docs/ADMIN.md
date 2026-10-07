# ADMIN — Wobble Vault

## Auth overview
- JWT 24h; delivered as httpOnly cookie `WV_AUTH` or `Authorization: Bearer <token>`.
- CSRF: cookie `XSRF-TOKEN` (not httpOnly), header `X-XSRF-TOKEN` on unsafe methods (POST/PUT/DELETE/PATCH).
- Stateless sessions; login rate limited 5 failed attempts → 15 min lock.
- Passwords hashed with BCrypt.

## Roles
`ADMIN`, `EMPLOYEE`, `MARKETING`, `PRODUCTION`, `SEWING`.

## Permissions (page names)
Trimmed set: `INVENTORY`, `ORDERS`, `SALES`, `SALES_ARCHIVE`, `ACCOUNTS`. Authorities = `ROLE_<role>` + each permission page name. `EMPLOYEES` is derived as ADMIN in UI logic (Accounts).

## Bootstrap admin
- Seeded when `app.bootstrap-admin.enabled=true` or local profile with H2 (if configured). Local dev seed values depend on `DataSeeder` configuration.
- Set via env vars: `APP_BOOTSTRAP_ADMIN_ENABLED`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_PASSWORD`.

## Creating/managing users
- Accounts page (previously Employees): register user with role, assign per-page permissions via modal, view user list.
- ADMIN requires email+password. Non-admin email/password may be nullable; such accounts cannot log in.

## Security DO-NOTs
- Never print `JWT_SECRET`, bootstrap passwords, or DB credentials in logs/output.
- State only where secrets live (env vars / deployment dashboard).
- Don’t commit `.env` files.

## 📎 Related
- [[INDEX]] — map of content
- [[OVERVIEW]] — summary, stack, quick-run
- [[ARCHITECTURE]] — data flow, schema, security model
- [[SETUP]] — running and deploying
- [[ADMIN]] — this page
