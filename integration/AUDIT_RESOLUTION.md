# NearNow Audit Resolution

The earlier audit identified paid-service coupling, payment/total inconsistencies, security issues, pagination contract mismatches, inactive-product management problems, and stale documentation. The current free-development baseline addresses those items as follows.

| Area | Resolution |
|---|---|
| Paid AI API | Removed; local deterministic embeddings + pgvector |
| Paid payment API | Removed from default project; Mock Payment Gateway only |
| Redis cost | Self-hosted `redis:7-alpine` container |
| Database cost | Self-hosted `pgvector/pgvector:pg16` container |
| JWT secret | Backend environment variable only |
| Flutter token | `flutter_secure_storage` |
| Role revocation | `authVersion` claim checked against current user |
| Auth abuse | Redis-backed login/register rate limit |
| CORS | Explicit configurable local origin list |
| Product status | Admin sees active + inactive products and can reactivate |
| Product JSON | Includes `active` |
| Pagination | Unified `PagedResponseDTO` with `totalPages` |
| Checkout pricing | Backend owns subtotal, delivery fee, grand total and re-pricing |
| Payment/order consistency | Mock payment amount is calculated from the same server pricing path |
| Cancellation | Inventory restored and paid mock payments become `REFUNDED` |
| Order transitions | Explicit state machine instead of arbitrary jumps |
| Semantic search abuse | Query length and result limits enforced |
| Cache invalidation | Product cache cleared after admin/vendor/warehouse-affecting changes where applicable |
| Notifications | Local asynchronous event/log; no paid provider |
| Monitoring | Prometheus/Grafana are optional local Docker profile |

## Deliberate zero-cost boundary

This project is designed to be demonstrable without paying for a cloud database, managed Redis, AI API, payment API, email API, SMS API, queue service, or hosted infrastructure.
