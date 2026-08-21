# NearNow Final Free-Development Status

## Included

- Spring Boot REST backend
- Flutter consumer REST wiring
- Next.js Admin / Warehouse Manager / Vendor portal
- PostgreSQL + pgvector self-hosted Docker setup
- Redis self-hosted Docker setup
- Local semantic-style search
- Local mock payment gateway
- JWT environment-secret architecture

## Major remediation completed

- Removed OpenAI API dependency and external embedding calls.
- Removed Razorpay Java SDK and Flutter Razorpay SDK from the default project.
- Added deterministic local embeddings with pgvector cosine ranking.
- Added Mock payment flow and payment refund state for cancelled paid orders.
- Made backend pricing authoritative for subtotal, delivery fee and grand total.
- Repriced cart items at checkout before order/payment amount calculation.
- Fixed JWT role-version invalidation.
- Added Redis-backed authentication rate limiting.
- Tightened CORS to an explicit local allowlist.
- Added admin-wide product listing so inactive products can actually be reactivated.
- Added `active` to the product JSON contract.
- Added `totalPages` to paginated API contracts.
- Converted admin/vendor pagination endpoints to the same DTO contract.
- Added API page-size limits.
- Added product-cache eviction for admin status changes.
- Added order-state transition validation.
- Added mock-payment refund state on cancellation.
- Removed simulated notification latency.
- Changed Flutter JWT persistence to `flutter_secure_storage`.
- Kept saved JWT on transient auto-login failures; only invalid-token responses clear it.
- Added self-hosted Redis and pgvector Docker images.
- Made Prometheus/Grafana optional local-only monitoring profile.
- Removed paid-service credentials from all examples and run instructions.

## Verification boundary

This environment does not contain the user's installed Maven/Flutter/Node dependency trees, so this bundle does not falsely claim a device/browser/runtime build was executed here. The source has been statically reconciled and the run order is provided for the user's local toolchain.
