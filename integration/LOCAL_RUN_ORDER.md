# NearNow — Free Local Run Order

## 0. Requirements

Install locally:

- JDK 17
- Maven 3.9+
- Docker Desktop
- Flutter SDK
- Node.js 20+

No paid account is required.

## 1. Start PostgreSQL + pgvector + Redis

From `backend/`:

```powershell
copy .env.example .env
```

Set a local password and JWT secret in `.env`:

```text
DB_PASSWORD=your-local-postgres-password
JWT_SECRET=use-a-random-secret-at-least-32-characters
PAYMENT_MODE=MOCK
```

Then:

```powershell
docker compose up -d postgres redis
```

This uses:

- `pgvector/pgvector:pg16` locally
- `redis:7-alpine` locally

No managed database/cache is used.

## 2. Start backend

```powershell
cd backend
mvn clean test
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080/api
```

The first startup creates/updates the local schema and the pgvector support table. Existing development payment columns from older NearNow versions are migrated away from the Razorpay-specific names.

## 3. Seed demo data

After creating an admin account, call the protected admin seed endpoint from the portal or your API client.

## 4. Flutter

If you already have the official platform folders in your existing Flutter project, keep them and merge the NearNow source files.

Android emulator:

```powershell
flutter run --dart-define=NEARNOW_API_BASE_URL=http://10.0.2.2:8080/api
```

Physical Android device:

```powershell
flutter run --dart-define=NEARNOW_API_BASE_URL=http://YOUR-PC-LAN-IP:8080/api
```

Flutter web:

```powershell
flutter run -d chrome --dart-define=NEARNOW_API_BASE_URL=http://localhost:8080/api
```

JWT tokens are stored with `flutter_secure_storage`, not SharedPreferences.

## 5. Free payment demonstration

Choose **Mock Online Payment** in checkout.

The flow is:

```text
Cart
 ↓
POST /api/payments/create-order
 ↓
Local MOCK payment reference
 ↓
Simulate success/failure in Flutter
 ↓
POST /api/payments/verify
 ↓
Server re-prices cart + validates stock
 ↓
Order created atomically
```

No Razorpay API is contacted and no real money is charged.

## 6. Semantic search

Semantic-style product search is local:

```text
query
 ↓
128-dimensional deterministic local embedding
 ↓
PostgreSQL pgvector cosine ranking
 ↓
active product results
```

There is no OpenAI key and no external AI API call.

## 7. Partner portal

```powershell
cd partner_portal
copy .env.example .env.local
npm install
npm run dev
```

Login with a backend user whose role is `admin`, `warehouse_manager`, or `vendor`.

The browser never receives the JWT signing secret. The portal uses an httpOnly JWT cookie and a server-side backend proxy.

## 8. End-to-end smoke test

1. Register/login a user.
2. Browse products/categories.
3. Search the catalog.
4. Add products to cart.
5. Confirm backend cart subtotal/delivery fee/grand total.
6. Place a COD order.
7. Place a Mock Online Payment order.
8. Cancel an eligible order and confirm mock payment status becomes `REFUNDED`.
9. Login to the Admin portal.
10. Delete and Reactivate a product from the Admin product list.
11. Assign roles and confirm old JWT privileges are invalidated.
12. Login as Warehouse Manager and test stock/pick-list operations.
13. Login as Vendor and test ownership-restricted product/order operations.
