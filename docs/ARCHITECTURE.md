# ARCHITECTURE — Wobble Vault

## 🌊 High-level data flow

```
 Browser (web)  ──┐
  (localhost)    └──▶ http://localhost:8081 (local) / https://<api-host> (prod)
                       │
                       ├── JPA / Hibernate (ddl-auto=update)
                       └── H2 (local) / PostgreSQL (prod)
```

1. The React app resolves its API base URL once (`discoverApiBaseUrl` in `frontend/src/services/api.js`):
   - If host is `localhost`/`127.0.0.1` **and** no `REACT_APP_API_URL` is set, it probes `http://localhost:8080`…`8090` (`GET /api/auth/health`) and uses the first that responds.
   - Otherwise it uses `REACT_APP_API_URL`.
2. All requests go through one axios instance (`api.js`) with `withCredentials: true`:
   - unsafe methods fetch `GET /api/auth/csrf` and attach `X-XSRF-TOKEN`;
   - JWT from `localStorage` (`wobble:accessToken`) attached as `Authorization: Bearer <token>`.
3. Backend authenticates (JWT filter), enforces `@PreAuthorize`, performs work, returns JSON.
4. Frontend routes protected by `ProtectedRoute` (auth) + `PermissionGuard` (permission).

## 🧩 Backend module layout (package `com.wobblevault.backend`)

- `config/` — `SecurityConfig`, `DataSeeder` (bootstrap admin + local admin seed), `PingController`.
- `entity/` — JPA entities (Inventory, Order, OrderItem, IncomeSource, User, Permission; others removed per plan).
- `features/<domain>/` — controller + service + DTO(s) + repository + request classes: `auth`, `users`, `inventory`, `orders`, `income`, `dashboard`.
- `security/` — `JwtProvider`, `JwtAuthenticationFilter` (reads Bearer or `WV_AUTH` cookie), `CustomUserDetailsService`.
- `exception/` — `GlobalExceptionHandler`, `ApiError`.
- `support/` — `IdempotencyService` (in-memory dedup).

Base path: `/`. All feature endpoints under `/api`.

## 🗄️ Database schema (JPA `ddl-auto=update`; Flyway off by default)

All PKs UUID.

| Table | Key columns / notes |
| --- | --- |
| `users` | `id` (UUID), `username` (unique), `email` (unique, nullable), `password` (nullable), `role` enum (`ADMIN,EMPLOYEE,MARKETING,PRODUCTION,SEWING`), `salary`, `version`, `created_at` |
| `permissions` | `id`, `user_id` FK→users, `page_name` (trimmed: `INVENTORY,ORDERS,SALES,SALES_ARCHIVE,ACCOUNTS`) |
| `inventory` | `id`, `brand` (required), `name` (shoe name, required), `size`, `quantity` (required), `price` (required), `notes`, `created_at` |
| `orders` | `id`, `job_order_no` (unique), `request_fingerprint` (unique), `customer_name`, `discount`, `price` (total), `payment`, `payment_method` (cash/gcash), `shop` (store/online), `order_date`, `notes`, `status` (ACTIVE/ARCHIVED/CANCELLED), `inventory_deducted`, `version`, `created_at` |
| `order_items` | `id`, `order_id` FK, `inventory_id`, `product_name`, `size`, `unit_price`, `quantity` (snapshot) |
| `income_sources` | `id`, `shop_type` (store/online), `payment_method` (cash/gcash), `income_date`, `customer_name`, `job_order_no`, `amount`, `reference_number`, `payment_category` (PAYMENT/LIQUIDATION), `remarks`, `created_at` |

Note: Entities for clients, customized orders, teams, attendance, returned items are excluded by design.

## 🔐 Security model
- **JWT**: HS512, HMAC key from `jwt.secret` (derived via SHA-512 in `JwtProvider`), expiry 86400000 (24h). Secret from `JWT_SECRET` env in prod.
- **Stateless**: `SessionCreationPolicy.STATELESS`.
- **Token delivery**: `Authorization: Bearer <token>` **or** httpOnly cookie `WV_AUTH` (`JwtAuthenticationFilter`).
- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` + plain `CsrfTokenRequestAttributeHandler` (no BREACH masking: cookie, `GET /api/auth/csrf` body, and `X-XSRF-TOKEN` header all carry the same token) — cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`. Ignored for `POST /api/auth/login`, `GET /api/auth/health`, `GET /api/auth/csrf`. Prod secure+SameSite per config.
- **Error statuses**: written directly as JSON (`ApiError` shape, no `sendError` re-dispatch) — `401` = missing/invalid auth, `403` = bad CSRF token or insufficient permission.
- **Authorization**: `@PreAuthorize` using `hasRole('ADMIN')` / `hasAuthority('<PAGE_NAME>')`. Authorities include `ROLE_<role>` + permission page names.
- **CORS**: `allowCredentials=true`; origins from `app.cors.origins` (+ localhost allowed when configured).
- **Login rate limiting**: in-memory; 5 failed → 15 min lock (keys by email|ip, ip|ip).
- **Passwords**: BCrypt.
- **Admin bootstrap**: when enabled, seeds ADMIN with default permissions (local profile seed supported).

## 🌐 Frontend ↔ backend wiring
- `frontend/src/services/api.js` — single axios instance; base-URL discovery (8080-8090 probe on localhost if no REACT_APP_API_URL); CSRF attach; Bearer attach.
- Per-domain services call `api.<verb>('/api/...')`.
- Routes: `/login`, `/dashboard`, `/inventory`, `/orders`, `/sales`, `/sales-archive`, `/accounts`.
- `AuthContext` loads CSRF then `/api/auth/me` to restore session from `localStorage` keys `wobble:accessToken`, `wobble:currentUser`.
- Permission utils aligned to trimmed page names.
- Keep-alive ping every 10 min when `REACT_APP_API_URL` set.

## 📎 Related
- [[INDEX]] — map of content
- [[OVERVIEW]] — summary, stack, quick-run
- [[ARCHITECTURE]] — this page
- [[SETUP]] — running and deploying
- [[ADMIN]] — auth details, users, roles, permissions
