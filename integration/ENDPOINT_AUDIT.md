# NearNow endpoint wiring audit

Generated from the supplied Phase 0–20 Spring Boot Controllers and the assembled frontend source.

## Consumer Flutter → backend

The consumer uses the shared `ApiClient`, whose base URL ends in `/api`; therefore service paths such as `/products` resolve to `/api/products`.

| Consumer feature | Backend endpoint family | Status |
|---|---|---|
| Auth | `/api/auth/login`, `/api/auth/register`, `/api/auth/me` | MATCH |
| Categories | `/api/categories/top-level`, `/api/categories/{parentId}/subcategories` | MATCH |
| Products | `/api/products`, `/category/{id}`, `/batch`, `/{id}`, `/featured`, `/barcode/{barcode}`, `/search` | MATCH |
| Cart | `/api/cart`, `/add`, `/remove/{id}`, `/update-qty/{id}`, `/clear` | MATCH |
| Wishlist | `/api/wishlist`, `/add/{productId}`, `/remove/{productId}` | MATCH |
| Address | `/api/addresses`, `/{id}`, `/{id}/set-default` | MATCH |
| Orders | `/api/orders`, `/{id}`, `/{id}/cancel` | MATCH |
| Reviews | `/api/reviews/product/{productId}` GET/POST | MATCH after Firebase removal |
| Admin seed | `/api/admin/seed` | MATCH after Firebase removal |
| Payment REST contract | `/api/payments/create-order`, `/api/payments/verify` | MATCH; SDK UI not included |

## Partner portal → backend

| Portal feature | Backend endpoint family | Status |
|---|---|---|
| Admin orders/status | `/api/admin/orders`, `/api/admin/orders/{id}/status` | MATCH |
| Admin products | `/api/products` + `/api/admin/products` CRUD/reactivate | MATCH |
| Admin role assignment | `/api/admin/users/{id}/role` | MATCH |
| Admin vendor upsert | `/api/admin/vendors` | MATCH |
| Admin seed | `/api/admin/seed` | MATCH |
| Warehouse pick lists | `/api/warehouse/pick-lists...` | MATCH |
| Warehouse stock | `/api/warehouse/stock` | MATCH |
| Vendor products | `/api/vendor/products...` | MATCH |
| Vendor orders | `/api/vendor/orders` | MATCH |
| Store CRUD | No required backend endpoint exists | BLOCKED — not fabricated |
| Vendor restock request | No required backend endpoint exists | BLOCKED — not fabricated |
| User directory/search | No list/search endpoint exists | BLOCKED — role assignment uses known ID |

## Backend endpoint inventory

The supplied backend exposes the following controller families:

- Auth: `/api/auth/**`
- Categories: `/api/categories/**`
- Products: `/api/products/**`
- Cart: `/api/cart/**`
- Wishlist: `/api/wishlist/**`
- Addresses: `/api/addresses/**`
- Orders: `/api/orders/**`
- Reviews: `/api/reviews/**`
- Payments: `/api/payments/**`
- Admin: `/api/admin/**`
- Warehouse: `/api/warehouse/**`
- Vendor: `/api/vendor/**`
- Rider: `/api/rider/**`

The detailed controller list is represented by the Java source in `backend/src/main/java/com/nearnow/*/*Controller.java`.

## Firebase check

No direct Firebase package/import usage remains in `consumer_flutter/lib`.

## Important verification boundary

This is a source-level contract audit. The execution environment used to assemble this bundle did not have Maven, Flutter or installed Node dependencies, so it does not claim a successful `mvn test`, `flutter analyze`, or Next.js production build.
