# Wobble Vault

Security notes:

- Set `JWT_SECRET` in your environment before starting the backend.
- If you want an initial admin on a fresh database, set `APP_BOOTSTRAP_ADMIN_ENABLED=true` plus `APP_BOOTSTRAP_ADMIN_EMAIL` and `APP_BOOTSTRAP_ADMIN_PASSWORD`.
- Local runs auto-seed a dev admin on H2 (`local` profile): `admin@wobblevault.local` / `Admin123!` (see `application-local.properties`).
- Swagger and actuator are restricted by the backend security config.
