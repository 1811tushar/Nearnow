# NearNow — Complete Free-Development Project

This bundle contains the three NearNow application surfaces:

1. `backend/` — Spring Boot REST backend
2. `consumer_flutter/` — REST-wired Flutter consumer application source
3. `partner_portal/` — Next.js Admin / Warehouse Manager / Vendor operations portal

## Hard requirement: ₹0 during development

The default configuration does **not** call any paid API or managed cloud service.

- PostgreSQL + pgvector: self-hosted Docker container
- Redis: self-hosted Docker container
- Spring Boot: local Docker/JVM
- Next.js: local Node process
- Flutter: local SDK/device/emulator
- Semantic search: deterministic local embeddings + pgvector; no OpenAI API
- Online payment demo: local MOCK gateway; no Razorpay API and no real money
- Notifications: local application event/log; no SMS/email provider
- Prometheus/Grafana: optional local Docker profile only; not started by default
- RabbitMQ/Celery: not used

The only values that must be supplied locally are your PostgreSQL password and a JWT secret. Neither is a paid service credential.

## Architecture

```text
Flutter Consumer ── JWT/REST ──> Spring Boot <── JWT/REST ── Next.js Portal
                                      │
                         ┌────────────┴────────────┐
                         │                         │
                  PostgreSQL + pgvector       Redis 7
                    self-hosted Docker       self-hosted Docker
```

## JWT rule

The JWT **signing secret is backend-only**:

```text
JWT_SECRET (host environment)
        ↓
Spring Boot signs/verifies JWT
        ↓
Flutter receives access token only
        ↓
flutter_secure_storage
```

The JWT secret is not stored in Flutter, Next.js, `application.properties`, Git, or a browser cookie.

## Start locally

See `integration/LOCAL_RUN_ORDER.md`.

## Flutter

The consumer bundle intentionally contains the REST-wired source. If your existing Flutter project already has its official `android/`, `ios/`, `web/`, `windows/`, `test/` and configuration folders, replace/merge the source from `consumer_flutter/` into that project rather than recreating those folders.

If a shell is needed, run:

```powershell
cd consumer_flutter
Set-ExecutionPolicy -Scope Process Bypass
.\BOOTSTRAP_FLUTTER_SHELL.ps1
flutter run
```

For a physical Android device:

```powershell
flutter run --dart-define=NEARNOW_API_BASE_URL=http://YOUR-PC-LAN-IP:8080/api
```

## Backend

```powershell
cd backend
copy .env.example .env
# edit .env with a local DB password and a random JWT secret

docker compose up -d postgres redis
mvn spring-boot:run
```

Or run the backend itself in Docker:

```powershell
docker compose up --build
```

The default payment mode is `MOCK` and costs ₹0.

## Partner portal

```powershell
cd partner_portal
copy .env.example .env.local
npm install
npm run dev
```

Default backend URL: `http://localhost:8080/api`.

## Optional monitoring

Prometheus/Grafana are included only as a local optional Docker profile:

```powershell
docker compose --profile monitoring up -d
```

They are not required for the application and are not managed cloud services.

## Final remediation baseline

This handover includes the targeted audit fixes:

- JWT signing secret remains server-side only via `JWT_SECRET`; Flutter stores only the issued access token in secure storage and the Next.js portal stores it in an httpOnly cookie.
- Redis is self-hosted in Docker; PostgreSQL/pgvector is self-hosted in Docker.
- Development payment mode is `MOCK`; no Razorpay/OpenAI credential is required.
- Flutter's shared API client is cross-platform and no longer imports `dart:io`.
- Flutter image/OCR search can explicitly use `/api/products/semantic-search`, backed by the local 128-dimensional embedding implementation and pgvector.
- Portal mutation failures are surfaced through the shared toast path and page-level error states remain in place.
- Admin dashboard metrics are calculated server-side, including orders created since Asia/Kolkata midnight and low-stock counts.
- Portal is intentionally light-only; this is an explicit operations-UI decision, not an unfinished dark-mode toggle.

See `integration/FINAL_REMEDIATION.md` for the remediation summary and `FREE_TIER.md` for the cost boundary.
