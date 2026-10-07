# OVERVIEW — Wobble Vault

Wobble Vault is a shoe-shop ERP (web-first, mobile-first ready). It tracks inventory (shoes by brand/name/size), manages orders with stock deduction and automatic income recording on payment, handles sales/liquidations, and provides role-based permission management via Accounts.

## Tech stack
- **Frontend**: React 19, Create React App (CRA), Axios, React Router. Mobile-first responsive styles.
- **Backend**: Spring Boot 4.0.6, Java 17, Spring Security (JWT + CSRF), JPA/Hibernate, H2 (local), PostgreSQL (prod).
- **Auth**: JWT (httpOnly cookie `WV_AUTH` or Bearer), CSRF (`XSRF-TOKEN` → `X-XSRF-TOKEN`), stateless, 5-fail lock 15 min.
- **Build/Tooling**: Maven wrapper, npm.

## Features
- **Inventory**: brand, shoe name, size, quantity, price, notes; CRUD + search + low-stock (<10).
- **Orders**: create (deducts stock, records income if payment>0), list by status (ACTIVE/ARCHIVED/CANCELLED), view, edit (re-syncs stock delta + income), delete (ADMIN). Status tabs: All (active), Archived, Cancelled.
- **Sales**: income entries from order payments; liquidation modal (LIQ-YYYYMMDD-NNNN); performance report; receipts/print; CSV export.
- **Sales Archive**: Receipts vs Liquidations, search, delete.
- **Accounts**: users with roles (ADMIN, EMPLOYEE, MARKETING, PRODUCTION, SEWING), per-page permissions (INVENTORY, ORDERS, SALES, SALES_ARCHIVE, ACCOUNTS).
- **Dashboard**: inventory count, low stock, orders by status, sales/liquidation/net this month.
- **Idempotency**: duplicate requests deduped by `request_fingerprint`.

## Folder layout
- `backend/backend/src/main/java/com/wobblevault/backend/` — API (config, entity, features, security, exception, support)
- `backend/backend/src/main/resources/` — application.properties (+ `local` / `prod` profiles); Flyway off by default
- `frontend/src/` — components, views, services, context, utils, hooks
- `docs/` — Obsidian-style wiki docs

## Quick run
- Backend (local): `cd backend/backend; .\mvnw.cmd spring-boot:run` → http://localhost:8081
- Frontend: `cd frontend; npm start` → http://localhost:3000
- Tests: backend `.\mvnw.cmd test`; frontend `npm test -- --watchAll=false`
- Build: backend `.\mvnw.cmd clean package`; frontend `npm run build`

## Notes
- Package renamed to `com.wobblevault.backend`. Cookie `WV_AUTH`. localStorage keys `wobble:accessToken`, `wobble:currentUser`.
- Payment captured once at order creation; no payment-update flow in order details. Cancelling order auto-restores stock.

## 📎 Related
- [[INDEX]] — map of content
- [[OVERVIEW]] — this page
- [[ARCHITECTURE]] — data flow, schema, security model
- [[SETUP]] — running and deploying
- [[ADMIN]] — users, roles, permissions
