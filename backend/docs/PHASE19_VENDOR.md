# NearNow — Phase 19: Vendor / Merchant Portal

## Status

**Phase:** 19  
**Feature:** Vendor / Merchant management  
**Implementation mode:** integrated into the supplied Phase 0-14 backend baseline  
**Runtime testing:** deferred because the supplied archive has no `pom.xml`, as required by the project constraints.

---

# 0. Verified-before-code findings

The baseline was read before writing the vendor code.

Verified:

- `auth.User` already owns authentication and a string `role` field.
- Public registration always creates `role = "user"` and was left unchanged.
- JWT authority generation already converts the stored role into `ROLE_<ROLE>`.
- `Product` has a flat `stock` field and is the correct existing entity to extend with a nullable vendor relationship.
- `Order`/`OrderItem` already preserve order-time product snapshots.
- Existing exception classes and `ApiResponse` are reused.
- Existing `SecurityConfig` uses explicit role-gated path matchers.

The vendor domain therefore extends the current model rather than introducing a second authentication system.

---

# 1. Problem first

Before Phase 19, a product has no ownership boundary between merchants.

That means there is no safe answer to:

> "Can this logged-in vendor edit this product?"

Phase 19 introduces:

```text
User(role=vendor)
        ↓
Vendor business profile
        ↓
Product.vendor
```

The ownership rule becomes:

```text
Authenticated vendor
        ↓
Vendor.id
        ↓
Product.vendor.id must match
```

A vendor therefore cannot edit another vendor's product even if they guess the product ID.

---

# 2. Checklist format

## Checkpoint 1 — ENTITY

### `Vendor`

Fields:

- `id`
- `user` — `@OneToOne` with existing `User`
- `businessName`
- `businessAddress`
- `gstNumber`
- `active`

`User` remains the identity. `Vendor` is only the merchant/business profile.

### `Product.vendor`

Existing `Product` receives:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "vendor_id")
private Vendor vendor;
```

The field is nullable so all existing seeded/legacy products remain valid.

---

## Checkpoint 2 — REPOSITORY

Created:

```text
VendorRepository
```

Existing repositories extended with:

```text
ProductRepository.findByVendorIdOrderByNameAsc(...)
OrderRepository.findDistinctOrdersContainingVendor(...)
```

The order query returns only orders containing products belonging to the authenticated vendor.

---

## Checkpoint 3 — DTOs

Created:

```text
VendorProfileRequestDTO
VendorResponseDTO
VendorProductUpdateRequestDTO
VendorProductResponseDTO
VendorOrderItemResponseDTO
VendorOrderResponseDTO
```

Deliberately excluded:

- `User.passwordHash`
- customer's full delivery address
- customer's phone number
- full Order entity
- another vendor's products

A vendor gets only the subset of an order containing that vendor's own products.

---

## Checkpoint 4 — SERVICE

`VendorService` implements:

- vendor profile read/update
- own-product listing
- own-product price update
- own-product stock update for legacy products
- warehouse-aware stock protection
- own-order listing
- ownership checks

### Warehouse reconciliation

If `StockLevel` rows exist for a product:

```text
StockLevel = inventory source of truth
Product.stock = aggregate
```

Therefore a vendor can change price, but cannot directly overwrite physical stock.

If no `StockLevel` exists yet, the product is still a legacy product and vendor stock updates can use `Product.stock`.

This prevents Phase 19 from undoing Phase 18's inventory authority.

---

## Checkpoint 5 — CONTROLLER

### Profile

```http
GET /api/vendor/profile
PUT /api/vendor/profile
```

### Products

```http
GET /api/vendor/products
PUT /api/vendor/products/{id}
```

Request example:

```json
{
  "price": 100.00,
  "salePrice": 89.00,
  "stock": 25
}
```

For warehouse-managed products, `stock` must match the current aggregate; actual stock changes go through `/api/warehouse/stock`.

### Orders

```http
GET /api/vendor/orders?page=0&size=20
```

The result is paginated.

---

## Checkpoint 6 — SECURITY

Added to `SecurityConfig`:

```java
.requestMatchers("/api/vendor/**").hasRole("VENDOR")
```

JWT classes were not recreated or redesigned.

The Service performs the second ownership check.

---

## Checkpoint 7 — WIRING

Admin provisioning was added because public signup must not self-promote a normal user into a vendor.

Admin endpoints:

```http
PUT /api/admin/users/{id}/role
PUT /api/admin/vendors
PUT /api/admin/products/{productId}/vendor/{vendorId}
```

Recommended provisioning order:

```text
1. Existing user registers normally
2. Admin assigns role = vendor
3. Admin creates/updates Vendor profile
4. Admin assigns products to that Vendor
5. Vendor logs in again to obtain a JWT containing ROLE_VENDOR
6. Vendor uses /api/vendor/**
```

---

# 3. Execution Sequence

## Step 0 — Pre-code Q&A

**Why not create a second vendor authentication table?**

Because `User` already owns credentials and JWT identity. Duplicating identity would create synchronization problems.

**Why not let registration accept `role=vendor`?**

Because that would let anyone self-assign an elevated role.

**Why not give vendors `/api/admin/**`?**

Because vendor ownership is narrower than admin authority.

**Why not let vendors modify warehouse stock directly?**

Because physical inventory has already been made a warehouse responsibility in Phase 18.

---

## Step 1 — Entity

Created `Vendor.java` and extended `Product.java` with nullable `vendor`.

## Step 2 — Repository

Created `VendorRepository`; added vendor-aware Product and Order queries.

## Step 3 — DTOs

Created request/response classes listed above.

## Step 4 — Service

Created `VendorService` with ownership and inventory-source checks.

## Step 5 — Controller

Created `VendorController` under `/api/vendor`.

## Step 6 — Security

Added explicit `ROLE_VENDOR` matcher.

## Step 7 — Wiring

Extended `AdminService`/`AdminController` for role/profile/product provisioning.

## Step 8 — Test

Deferred because `pom.xml` is absent from the supplied archive.

## Step 9 — Next

Phase 20 Rider/Fleet can now use the same multi-role pattern.
