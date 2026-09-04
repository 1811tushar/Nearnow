# NearNow Backend — Production Environment Variables Checklist

Use this when setting up the Render Web Service (or any host). Every
variable below maps directly to a placeholder in `application.properties`.
Nothing here should ever be committed to git — all of it goes into the
hosting platform's own "Environment Variables" dashboard.

---

## 🔴 REQUIRED — app will fail to start without these (no default exists)

| Variable | What it is | Where to get it |
|---|---|---|
| `DB_PASSWORD` | Postgres password | Render Postgres dashboard → Connection Info |
| `JWT_SECRET` | Signing key for access + refresh tokens | Generate a long random string (32+ chars). Never reuse the local dev one. |
| `MAIL_USERNAME` | Gmail address that sends OTP emails | Your Gmail address |
| `MAIL_APP_PASSWORD` | 16-char Gmail App Password (NOT your real Gmail password) | Google Account → Security → 2-Step Verification → App Passwords |

## 🟡 REQUIRED FOR CORRECT BEHAVIOR — has a default, but the default is wrong in production

| Variable | Local dev default | What to set in production |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` — this alone disables `/api/admin/seed` (see `AdminSeedController`'s `@Profile("dev")`), which must never exist in production |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/nearnow_db` | Render Postgres's Internal Database URL (from its dashboard) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Whatever Render's Postgres dashboard shows (often not literally `postgres`) |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Your Redis provider's host (e.g. Upstash) |
| `SPRING_DATA_REDIS_PORT` | `6379` | Your Redis provider's port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8080,http://10.0.2.2:8080` | Your real Vercel URL(s), e.g. `https://nearnow-portal.vercel.app` — **the app will otherwise silently reject every request from the deployed portal with a CORS error that looks like a bug in the frontend** |

## 🟢 PLATFORM-HANDLED — do not set manually on Render

| Variable | Note |
|---|---|
| `PORT` | Render injects this automatically at runtime. `application.properties` now reads `${PORT:${SERVER_PORT:8080}}` — Render's `PORT` takes priority over the old `SERVER_PORT` variable, so no manual action needed, but do NOT set `SERVER_PORT` to a fixed value on Render or it becomes a confusing (unused) leftover. |

## ⚪ OPTIONAL — safe to leave at defaults initially

| Variable | Default | When you'd change it |
|---|---|---|
| `JWT_EXPIRATION_MS` | 1 hour | Rarely needs changing |
| `JWT_REFRESH_EXPIRATION_MS` | 30 days | Rarely needs changing |
| `PAYMENT_MODE` | `MOCK` | Change to `RAZORPAY` once that integration is done (separate task) |

---

## Pre-deploy sanity checks (do these once, right before going live)

- [ ] `SPRING_PROFILES_ACTIVE=prod` is set → confirm `POST /api/admin/seed` returns 404 (not 403) on the deployed URL
- [ ] `CORS_ALLOWED_ORIGINS` includes the exact deployed Vercel URL (including `https://`, no trailing slash)
- [ ] `JWT_SECRET` in production is DIFFERENT from the one used locally/in CI — a leaked local secret should never be able to forge production tokens
- [ ] Confirm the deployed backend's `/actuator/health` returns `{"status":"UP"}` — if `DOWN`, check DB/Redis connectivity env vars first
