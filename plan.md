# Wobble Vault - Build Plan

This document is the plan verbatim as specified.

## Context / Source
- The blueprint is this repo: `C:\Users\Rosa\Desktop\Projects\Verdida Sports Apparel`
  (React 19 + CRA frontend, Spring Boot 4.0.6 / Java 17 backend, JWT+CSRF, H2 local
  / Postgres prod). Read its `AGENTS.md` and `docs/INDEX + ARCHITECTURE` first.
- Read-only. NEVER modify the Verdida repo. Work only inside the new folder.

## STEP 1 — SCAFFOLD (Phase 0)
- Copy the entire Verdida repo to `C:\Users\Rosa\Desktop\Projects\Wobble Vault`,
  DELETING `.git` inside it, then `git init` a fresh repo there.
- Delete Electron: `frontend/electron/`, `frontend/electron-builder.yml`, and the
  `electron:*` scripts + `electron`/`electron-builder` devDependencies in package.json.
- Rebrand: productName "Wobble Vault"; backend package `com.wobblevault.backend`;
  auth cookie `WV_AUTH`; localStorage keys `wobble:accessToken`/`wobble:currentUser`;
  new H2 DB names; login page + `public/` assets (logo/manifest/index.html titles).
- Write `plan.md` at the project root containing this plan verbatim, then rewrite
  `AGENTS.md` and `docs/` (Obsidian-style, see source docs) for Wobble Vault.

## LOCKED DECISIONS
1. Order status = ACTIVE | ARCHIVED | CANCELLED. Tabs All(=active only)/Archived/
   Cancelled. Creating an order deducts inventory stock; cancelling auto-restores stock.
2. Payment is captured ONCE at order creation; editing the order re-syncs its income
   entry (there is NO payment-update flow in order details).
3. Accounts page keeps VSA roles + per-page permissions UI (just renamed "Accounts").
4. No Electron. No customized-orders, clients, teams, attendance, or returned-items.

## STEP 2 — BACKEND (package com.wobblevault.backend)
- KEEP: config/SecurityConfig (new cookie names), PingController, security/*, exception/*,
  support/IdempotencyService, features/auth/*, JobOrderNumberService, income LIQ- generator +
  syncOrderPayment, inventory CRUD, users+permissions.
- Inventory entity: brand(required), name=shoe name(required), size, quantity(required),
  price(required), notes. DROP itemType/jerseyType/number/shop.
- Order: jobOrderNo, requestFingerprint, customerName, items[], discount, price(total),
  payment, paymentMethod, shop, orderDate, notes, status, inventoryDeducted, @Version,
  createdAt. DROP team/freebie/downPayment/pickup.
- OrderItem: inventoryId+productName+size+unitPrice+quantity (snapshot name/size/price so
  deleted inventory rows still render). inventoryId enables stock reconciliation — do NOT
  copy Verdida's fuzzy retail-label inventory scan.
- IncomeSource: shopType(store|online), paymentMethod(cash|gcash), incomeDate, customerName,
  jobOrderNo, amount, referenceNumber, paymentCategory(PAYMENT|LIQUIDATION), remarks.
- Permission.PageName trimmed to: INVENTORY, ORDERS, SALES, SALES_ARCHIVE, ACCOUNTS.
- DROP entities: Client, CustomizedOrder, CustomizedOrderItem, Team, TeamPlayer,
  EmployeeAttendance, ReturnedItem (+ their features/clients, teams, attendance,
  returneditems, customizedorders).
- Endpoints: inventory CRUD+search+low-stock; orders POST(default ACTIVE, deduct stock,
  record income if payment>0), paged GET with ?status=, /{id}, /job-order-no, date-range,
  year-month, PUT (re-sync stock delta + income), DELETE(ADMIN); income list/date-range/
  delete; dashboard stats (inventory count, low stock<10, orders by status, sales,
  liquidation, net this month); users/permissions unchanged shape.
- Add server-side validation of status/shop/paymentMethod values (improvement over source).
- Config: application*.properties (H2 local :8081 / Postgres prod :8080, JWT_SECRET env,
  CORS_ORIGINS, APP_BOOTSTRAP_ADMIN_*). Fresh schema via ddl-auto=update, Flyway off.

## STEP 3 — FRONTEND CORE
- Routes: /login, /dashboard, /inventory, /orders, /sales, /sales-archive, /accounts.
- KEEP: api.js discovery+CSRF, contexts, Sidebar/Navbar(768px collapse), DataTable, Modal,
  ConfirmModal, SearchField, guards, DashboardLayout, LoginView(rebranded).
- Inventory: form = brand, shoe name, size, qty, price, notes; overview table = Brand |
  Shoe Name | Size | Qty (+actions); search by name/brand.
- Orders (rewrite): customer name; product rows picked from inventory (brand/name/size/
  stock) with size, unit price, qty, subtotal; "+ Add another product"; discount; computed
  total; payment; payment method(Cash|Gcash); balance; shop(Wobble Store default|FB Page);
  order date; notes; total-computation block. Overview cols = Order No | Customer | Date |
  Actions. Details dropdown: "Hide/Archive order" | "Order is cancelled" (reverse action
  when viewing archived). No returned-items/payment-update UI.
- Dashboard: simplified stat cards.

## STEP 4 — SALES / SALES ARCHIVE / ACCOUNTS
- Sales (derive from source SourceIncomeView, stripped): income entries from order payments,
  shop=Store|Online(FB), payment=Cash|Gcash; liquidation modal (date, amount, reason,
  ref LIQ-YYYYMMDD-NNNN); performance report; print/PDF receipts; Excel export.
- Sales Archive (rename FinanceArchiveView): Receipts vs Liquidations, search, delete.
- Accounts (rename EmployeesView): register user w/ role, permissions modal using the new
  page names, user list.
- Update services + utils/permissions.js; delete client/customized/attendance/teams/
  returned-item services.

## STEP 5 — MOBILE-FIRST CSS PASS
- Card-based lists for overview tables; full-screen modals <768px; sticky action bars;
  ≥44px touch targets; single-column forms; the existing 768px breakpoint pattern extended
  down to 480px; viewport meta. Use the `design-taste-frontend` skill if copied into the
  new repo for the polish.

## STEP 6 — TESTS / VERIFY / DOCS
- Port: App smoke, LoginView, liquidationUtils tests. New: backend OrderServiceTest
  (create deducts + records income, cancel restores), InventoryServiceTest, UserServiceTest,
  IdempotencyServiceTest; frontend tests for order total computation + archive/cancel and
  inventory add-item.
- Verify after each phase: backend `.\mvnw.cmd test` (from backend/backend), frontend
  `npm run build`, `$env:CI="true"; npm test -- --watchAll=false`.
- No source-code comments unless asked. Keep AGENTS.md terse (~40-60 lines) and in sync.

## STEP 7 — ANDROID APP (`WobbleVault/`, added later)
- Build fix: Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, compose BOM 2026.06.01, compileSdk 36
  (BOM ≥2026.07 needs SDK 37 + AGP 9). Brand theme/icons from BrandTheme.css + wvlogo.png.
- Integration: Retrofit+Gson against `BuildConfig.BASE_URL` (Render), login → stored JWT →
  `GET /api/auth/me` restore; Bearer + `X-XSRF-TOKEN` (OkHttp CookieJar double-submit).
- Screens: splash/restore, login (401/429 messages), permission-gated menu, dashboard stats,
  inventory read-only list (page=0&size=100, client-side search). ADMIN or INVENTORY gate.

## DONE
Phase 0..7 complete, all builds/tests green, plan.md + docs written. Report what you
built, what you skipped, and any deviations.
