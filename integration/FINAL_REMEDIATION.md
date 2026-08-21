# Final remediation pass

- Flutter API client no longer imports `dart:io`, so web builds do not depend on a VM-only library.
- Flutter image/OCR search now has an explicit semantic-search path backed by the local embedding service and PostgreSQL/pgvector.
- Spring CORS reads the single `app.cors.allowed-origins` configuration property rather than a second configuration source.
- Partner portal API failures are surfaced through the shared toast event path while retaining page-level error states.
- Portal accessibility was tightened for navigation controls; light-only mode is an explicit design decision.
- Unused `react-hook-form`, `zod`, and `recharts` dependencies were removed rather than left as dead weight.
- Admin dashboard now has a backend-owned `/api/admin/dashboard` summary with a real Asia/Kolkata start-of-day calculation instead of using the current page total as a proxy for today's orders.

## Runtime verification
The archive is source-complete, but final machine verification must be run with JDK/Maven, Flutter SDK, and Node/npm installed.
