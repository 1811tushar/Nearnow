# NearNow Partner Operations Portal

Next.js + TypeScript operations portal for the three verified backend roles: `admin`, `warehouse_manager`, and `vendor`.

## Verified backend source
Built against the supplied Phase 0–20 Spring Boot backend. The backend is the source of truth; endpoint paths and DTO fields were read from the actual Controller/DTO classes before wiring.

## Fully wired screens
### Auth
- `/login` → `POST /api/auth/login`
- JWT is stored in an httpOnly cookie by the Next.js session route.
- Role routing: `admin` → `/admin`, `warehouse_manager` → `/warehouse`, `vendor` → `/vendor`.
- Browser API calls go through `/api/backend/*`; the Next.js proxy reads the httpOnly JWT cookie and adds `Authorization: Bearer ...`.

### Admin
- `/admin` → real product/order APIs for operational metrics and recent orders.
- `/admin/orders` → `GET /api/admin/orders`, `PUT /api/admin/orders/{id}/status`.
- `/admin/products` → `GET /api/admin/products`; create/update/delete/reactivate via `/api/admin/products`.
- `/admin/users` → `GET /api/admin/users` plus role assignment via `PUT /api/admin/users/{id}/role`.
- `/admin/vendors` → `GET /api/admin/vendors` plus vendor upsert via `PUT /api/admin/vendors`.
- `/admin/stores` → `GET/POST/PUT/PATCH /api/admin/stores...` for store management and manager assignment.
- `/admin/seed` → `POST /api/admin/seed`.

### Warehouse
- `/warehouse` → `GET /api/warehouse/pick-lists`.
- `/warehouse/pick-lists/[id]` → verified pick endpoint with barcode validation and complete endpoint.
- `/warehouse/stock` → `GET/PUT /api/warehouse/stock`.
- Warehouse home → vendor restock queue via `GET /api/warehouse/restock-requests` and `PUT /api/warehouse/restock-requests/{id}/status`.

### Vendor
- `/vendor` → `GET /api/vendor/products` quick-glance metrics.
- `/vendor/products` → `GET /api/vendor/products`, `PUT /api/vendor/products/{id}`.
- `/vendor/orders` → `GET /api/vendor/orders`.
- Vendor restock request → `POST /api/vendor/products/{id}/restock-request`, plus vendor request history.

## Backend capability coverage
All portal screens in this bundle now target verified backend endpoints. No blocked placeholder screens remain for Stores, Users, Vendors, or Vendor Restock.

## Backend endpoint inventory used
- Auth: `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me`
- Admin: `POST /api/admin/seed`, `GET/POST/PUT /api/admin/products`, `PATCH /api/admin/products/{id}/reactivate`, `DELETE /api/admin/products/{id}`, `GET /api/admin/orders`, `PUT /api/admin/orders/{id}/status`, `GET/PUT /api/admin/users`, `GET/PUT /api/admin/vendors`, `GET/POST/PUT/PATCH /api/admin/stores`, `PUT /api/admin/products/{productId}/vendor/{vendorId}`, `POST /api/admin/riders/assign/{orderId}`
- Warehouse: `GET /api/warehouse/pick-lists`, `PUT /api/warehouse/pick-lists/{id}/items/{itemId}/pick`, `PUT /api/warehouse/pick-lists/{id}/complete`, `GET/PUT /api/warehouse/stock`
- Vendor: `GET/PUT /api/vendor/profile`, `GET/PUT /api/vendor/products`, `POST /api/vendor/products/{id}/restock-request`, `GET /api/vendor/restock-requests`, `GET /api/vendor/orders`
- Catalog read APIs used by admin: `GET /api/products`, `GET /api/products/search`, `GET /api/categories/top-level`, `GET /api/categories/{parentId}/subcategories`

## Design decisions
- Dark green/near-black operational chrome + lime action accent: strong contrast and rapid scanning.
- Cards are restrained; tables carry density where operators need it.
- Large touch targets are used for warehouse pick actions.
- Backend messages are surfaced instead of generic client errors.
- Empty, loading, error and retry states are explicit.
- No money is recomputed from raw line items; displayed order totals are backend-provided values.
- No fake data or invented endpoints are used.
- Dark mode was deliberately not added: this first release optimizes for a high-contrast light operations workspace; a full dark theme should be introduced as a complete token set rather than partial component overrides.

## Run
```bash
npm install
npm run dev
```
Set `NEXT_PUBLIC_API_BASE_URL` to the backend API base, e.g. `http://localhost:8080/api`.


## UI/UX decisions
- Light theme is intentional for the operations portal; dark mode is not enabled because this is a high-density operational workspace. The decision is explicit rather than an unfinished feature.
- API failures are surfaced through a shared toast event path in addition to local inline error states.
- The portal uses the backend as the security boundary; middleware role routing is UX protection only.
