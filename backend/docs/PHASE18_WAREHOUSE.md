# NearNow — Phase 18: Warehouse / Dark-Store Operations

## Status

**Phase:** 18  
**Feature:** Warehouse / Dark-Store Operations  
**Implementation mode:** new `com.nearnow.warehouse` package + merge-safe proposed edits  
**Runtime testing:** deferred exactly as required by the master prompt

---

# 0. Verified-before-code findings

The supplied archive was inspected before generating the warehouse package.

Verified existing structures:

- `com.nearnow.common.dto.ApiResponse`
  - static `success(data)`, `success(data, message)`, `error(message)`
- `com.nearnow.common.dto.PagedResponseDTO`
- Existing exception types:
  - `ResourceNotFoundException`
  - `DuplicateResourceException`
  - `InvalidCredentialsException`
  - `InvalidOperationException`
- Existing JWT stack:
  - `JwtUtil`
  - `JwtAuthFilter`
- Existing `SecurityConfig`
  - explicit endpoint matcher style
  - `ROLE_ADMIN` is already wired
  - all unlisted routes default to authenticated
- `User`
  - `role` is a `String`
  - current registration defaults to `"user"`
  - JWT converts the stored role into `ROLE_<UPPERCASE_ROLE>`
- `Product`
  - existing flat `int stock`
  - product barcode is server-owned
- `Order`
  - contains `inventoryReserved` and `stockRestored`
  - status lifecycle is `PLACED -> PACKED -> OUT_FOR_DELIVERY -> DELIVERED`
- `OrderItem`
  - references `Product`
  - stores quantity and order-time product snapshot
- `Address`
  - already contains latitude/longitude
- `OrderService.placeOrder()`
  - currently locks `Product` rows and decrements `Product.stock`
  - this is the exact integration point that must change when warehouse inventory becomes authoritative
- `AdminService`
  - currently creates/updates `Product.stock`
  - this must be prevented from overwriting warehouse-managed aggregate stock

## Important archive limitation

The supplied backend archive does **not contain `pom.xml`**, although the existing `Dockerfile` expects one.

Therefore this phase was produced against the actual Java source, but a truthful Maven compile/test cannot be run from this archive until the project's correct `pom.xml` is restored.

---

# 1. Architecture decision

## Problem first

Before Warehouse, inventory is:

```text
Product
  └── stock = 100
```

That works for one logical inventory pool, but it cannot answer:

> "Store A has 20 units and Store B has 80 units."

Warehouse operations require:

```text
Product
   ↑
StockLevel
   ├── Store A → 20
   └── Store B → 80
```

## Locked decision

**`StockLevel` becomes the authoritative per-store inventory.**

`Product.stock` is retained temporarily as a **derived compatibility aggregate**:

```text
Product.stock
    =
SUM(all StockLevel.quantity for that product)
```

It is NOT an independent source of truth once the product has warehouse stock rows.

Why retain it?

Because the existing customer-facing product DTOs and legacy code already expose/use `Product.stock`. Removing it immediately would create a much larger unrelated migration.

Therefore:

```text
StockLevel = source of truth
Product.stock = compatibility aggregate
```

This is an explicit reconciliation rule, not two competing inventories.

---

# 2. Checklist format

## Checkpoint 1 — ENTITY

### `Store`

Fields:

- `id`
- `name`
- `addressLine`
- `city`
- `pincode`
- `latitude`
- `longitude`
- `capacity`
- `operatingHoursStart`
- `operatingHoursEnd`
- `active`
- `warehouseManager`

### Why `warehouseManager` is present

The security requirement says a warehouse manager must only see their assigned store.

The existing `User` model has no store-assignment field.

Instead of modifying `User`, the ownership relationship lives on `Store`:

```text
Store
  └── warehouseManager → User
```

This keeps authentication data separate from warehouse ownership.

Role assignment remains an admin/direct-DB operation; it is not self-service.

---

### `StockLevel`

Fields:

- `id`
- `store`
- `product`
- `quantity`

Database constraint:

```text
(store_id, product_id) UNIQUE
```

Therefore one product has at most one inventory row per store.

---

### `PickList`

Fields:

- `id`
- `order`
- `store`
- `status`
- `items`

`order_id` is unique because one order gets one warehouse pick list.

---

### `PickListItem`

Fields:

- `id`
- `pickList`
- `product`
- `quantity`
- `picked`

A separate item entity is required because one pick list contains multiple products and each item has independent scan/pick state.

---

## Checkpoint 2 — REPOSITORY

### `StoreRepository`

Provides:

```text
findByActiveTrueOrderByIdAsc()
findByWarehouseManagerIdAndActiveTrue(...)
```

The first gives candidate stores for routing.

The second enforces manager-to-store ownership.

### `StockLevelRepository`

Provides:

```text
findByStoreIdAndProductId(...)
findByStoreIdOrderByProduct_NameAsc(...)
findByStoreIdAndProductIdIn(...)
findByStoreIdAndProductIdInForUpdate(...)
decrementQuantity(...)
sumQuantityByProductId(...)
existsByProductId(...)
```

The important one is the pessimistic-lock query.

Analogy:

> Before two cashiers both sell the last bottle, one cashier must lock the shelf record before checking it.

That is why warehouse allocation uses a DB row lock.

The decrement query also contains:

```text
quantity >= requestedQuantity
```

so the database itself refuses an overselling decrement.

### `PickListRepository`

Provides:

```text
findByStoreIdOrderByIdDesc(...)
findByOrderId(...)
findOwnedByManager(...)
```

`findOwnedByManager()` is deliberately scoped through:

```text
pickList.store.warehouseManager.id
```

so a manager cannot fetch another store's pick list merely by guessing its ID.

---

## Checkpoint 3 — DTOs

Created DTOs:

```text
StoreResponseDTO
StockAdjustmentRequestDTO
StockLevelResponseDTO
PickItemRequestDTO
PickListItemResponseDTO
PickListResponseDTO
```

### Deliberately excluded

From warehouse responses:

- `User.passwordHash`
- warehouse manager's private identity/auth fields
- complete `Order` entity
- JPA entities themselves

The controller returns DTOs only.

### Barcode request

`PickItemRequestDTO` accepts:

```json
{
  "scannedBarcode": "..."
}
```

The server compares this against the product's server-owned barcode.

The client therefore cannot simply say:

```json
{
  "picked": true
}
```

and bypass product validation.

---

# Checkpoint 4 — SERVICE

## A. Nearest store with stock

Problem:

```text
User orders:
Milk x2
Bread x1
```

There may be:

```text
Store A = 1 km away
Milk = 2
Bread = 0

Store B = 2 km away
Milk = 10
Bread = 5
```

The system must select Store B.

The implementation:

1. Get all active stores.
2. Calculate Haversine distance from order delivery coordinates.
3. Sort nearest → farthest.
4. For each candidate:
   - lock relevant `StockLevel` rows
   - check required quantities
   - reserve atomically
5. First store satisfying the complete basket wins.

No PostGIS is introduced because the current problem does not justify another database extension.

---

## B. Haversine calculation

Existing `Address` and `DeliveryAddressSnapshot` already provide:

```text
latitude
longitude
```

Therefore no external maps API is required for this phase.

Distance is calculated server-side with the standard Haversine formula.

---

## C. Stock reservation

The operation is transactional:

```text
Order transaction
      ↓
Warehouse allocation
      ↓
lock StockLevel rows
      ↓
check quantities
      ↓
decrement StockLevel
      ↓
create PickList
      ↓
sync Product.stock aggregate
      ↓
commit
```

If allocation fails:

```text
ROLLBACK
```

The order and stock reservation do not partially survive.

---

## D. Pick-list generation

After successful reservation:

```text
Order #123
   ↓
PickList
   ├── Milk × 2
   └── Bread × 1
```

Status starts:

```text
PENDING
```

First successful barcode scan:

```text
IN_PROGRESS
```

All items picked:

```text
COMPLETED
```

When completed, an existing `PLACED` order moves to:

```text
PACKED
```

This connects the new warehouse workflow to the existing Order lifecycle without adding fields to `Order`.

---

## E. Barcode validation

For every pick:

```text
scannedBarcode == Product.barcode
```

If false:

```text
InvalidOperationException
→ HTTP 400
```

If true:

```text
PickListItem.picked = true
```

---

## F. Stock adjustment

Warehouse manager can set an absolute physical count:

```json
{
  "productId": 15,
  "quantity": 42
}
```

This is intentionally an absolute quantity rather than a client-supplied "delta".

The physical warehouse count is the source of the adjustment.

After saving:

```text
StockLevel.quantity = 42
Product.stock = SUM(StockLevel.quantity)
```

---

# Checkpoint 5 — CONTROLLER

All endpoints are under:

```text
/api/warehouse
```

### Pick lists

```http
GET /api/warehouse/pick-lists
```

Returns only the manager's assigned store's pick lists.

---

```http
PUT /api/warehouse/pick-lists/{id}/items/{itemId}/pick
```

Request:

```json
{
  "scannedBarcode": "123456789"
}
```

---

```http
PUT /api/warehouse/pick-lists/{id}/complete
```

Completes only when every pick-list item has been scanned.

---

### Stock

```http
GET /api/warehouse/stock
```

Returns stock for the manager's assigned store.

---

```http
PUT /api/warehouse/stock
```

Request:

```json
{
  "productId": 15,
  "quantity": 42
}
```

---

# Checkpoint 6 — SECURITY

All five endpoints require:

```text
ROLE_WAREHOUSE_MANAGER
```

SecurityConfig addition:

```java
.requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE_MANAGER")
```

The JWT system already converts:

```text
warehouse_manager
```

into:

```text
ROLE_WAREHOUSE_MANAGER
```

No changes to:

```text
JwtUtil.java
JwtAuthFilter.java
```

are required.

### Ownership is NOT delegated to the URL

This is insufficient:

```text
ROLE_WAREHOUSE_MANAGER
+
/api/warehouse/pick-lists/999
```

The Service also checks:

```text
pickList.store.warehouseManager == authenticated user
```

Likewise, stock operations first resolve the authenticated user's assigned store.

This gives defense in depth:

```text
SecurityConfig
    ↓
role check

WarehouseService
    ↓
ownership check
```

---

# Checkpoint 7 — WIRING

New package dependencies:

```text
warehouse
   ├── auth.User/UserRepository
   ├── product.Product/ProductRepository
   └── order.Order
```

### Existing integration point

Current:

```text
OrderService.placeOrder()
```

still performs:

```text
Product row lock
Product.stock validation
Product.stock decrement
```

That must NOT remain as the warehouse source of truth.

The parent project should replace that inventory block with:

```java
warehouseService.reserveForOrder(savedOrder);
```

The warehouse service then handles:

```text
nearest Store
    ↓
StockLevel locking
    ↓
StockLevel decrement
    ↓
PickList creation
    ↓
Product.stock aggregate synchronization
```

This is a **merge-safe proposed edit**, not a full replacement of `OrderService.java`.

---

# 3. Execution Sequence

## Step 0 — Pre-code Q&A

### Q: Why not create a separate warehouse microservice?

Because the locked scale decision is a monolith with selective upgrades.

The warehouse domain has strong transactional coupling with:

```text
Order
Product
User
```

Keeping it in the existing monolith avoids distributed transactions and unnecessary infrastructure.

### Q: Why not PostGIS?

Current store counts do not justify it.

Simple Haversine calculation over the active-store candidate list is sufficient.

### Q: Why not Redis for inventory?

Inventory correctness depends on PostgreSQL transaction/row-lock semantics.

Redis can later assist caching, but it should not become the source of truth for stock.

### Q: Why not RabbitMQ/Kafka?

Pick-list creation is part of the checkout transaction and must succeed atomically with inventory reservation.

The locked architecture explicitly avoids introducing a broker at this scale.

---

## Step 1 — Entity

Created:

```text
warehouse/
├── Store.java
├── StockLevel.java
├── PickList.java
├── PickListItem.java
└── PickListStatus.java
```

---

## Step 2 — Repository

Created:

```text
StoreRepository.java
StockLevelRepository.java
PickListRepository.java
```

---

## Step 3 — DTO

Created:

```text
StoreResponseDTO.java
StockAdjustmentRequestDTO.java
StockLevelResponseDTO.java
PickItemRequestDTO.java
PickListItemResponseDTO.java
PickListResponseDTO.java
```

---

## Step 4 — Service

Created:

```text
WarehouseService.java
```

Business rules implemented:

- nearest-store selection
- per-store stock validation
- pessimistic inventory locking
- atomic stock decrement
- pick-list creation
- barcode validation
- pick progression
- pick completion
- order transition to PACKED
- warehouse-manager ownership
- stock adjustment
- Product.stock aggregate synchronization
- legacy-order stock restoration fallback

---

## Step 5 — Controller

Created:

```text
WarehouseController.java
```

No entity is returned directly.

Every response uses:

```java
ApiResponse<T>
```

matching the existing project contract.

---

## Step 6 — Security

**Do not replace `SecurityConfig.java`.**

Add only the proposed matcher shown in the merge section below.

No changes are required to JWT classes.

---

## Step 7 — Wiring

Two existing areas require explicit merge work:

1. `OrderService.java`
2. `AdminService.java`

`Product.java` receives a documentation/reconciliation clarification, not a structural redesign.

---

## Step 8 — Test

Deferred exactly as required by the master prompt.

Static source verification was performed.

The archive's missing `pom.xml` prevents a truthful Maven compile/test.

---

## Step 9 — Next

Stop here.

Do **not** begin Phase 19 until Phase 18 is merged and verified by the project owner.

---

# 4. Proposed edits to existing files

These are intentionally NOT full-file replacements.

## A. `SecurityConfig.java`

Inside the existing `.authorizeHttpRequests(...)` chain, add:

```java
.requestMatchers("/api/warehouse/**").hasRole("WAREHOUSE_MANAGER")
```

Place it alongside the existing `/api/admin/**` role-gated rule.

Do NOT replace or restructure the existing authorization chain.

---

## B. `OrderService.java`

### 1. Add import

```java
import com.nearnow.warehouse.WarehouseService;
```

### 2. Add dependency

```java
private final WarehouseService warehouseService;
```

### 3. Constructor

Add `WarehouseService warehouseService` to the constructor and assign it.

### 4. Replace the current checkout inventory block

The current code performs:

```text
Product row locking
        ↓
Product.stock validation
        ↓
Product.stock decrement
```

That entire block beginning at the comment:

```java
// Lock every product row in deterministic id order.
```

through the current:

```java
order.setInventoryReserved(true);
```

should be replaced by the warehouse integration after the order has been saved:

```java
Order saved = orderRepository.save(order);

warehouseService.reserveForOrder(saved);

// Cart cleared as part of the same transaction as order-creation.
cartItemRepository.deleteByCartId(cart.getId());
```

The existing `OrderService` transaction remains the transaction boundary.

### Why the order is saved first

`PickList` references `Order`.

The order must therefore have its database identity before the warehouse service creates the pick list.

If warehouse allocation subsequently fails, the surrounding `@Transactional` checkout rolls the order insert back.

---

## C. `OrderService.java` cancellation wiring

For new warehouse-managed orders, cancellation should restore `StockLevel`, not only `Product.stock`.

The warehouse package therefore provides:

```java
warehouseService.restoreReservedStock(order)
```

Recommended parent-session pattern:

```java
if (order.isInventoryReserved() && !order.isStockRestored()) {
    boolean warehouseManaged =
            warehouseService.restoreReservedStock(order);

    if (!warehouseManaged) {
        // Existing legacy-order restoration path.
        restoreStock(order);
    }
}
```

This preserves older orders that existed before warehouse allocation while making new warehouse orders use their actual store inventory.

---

## D. `AdminService.java`

Current admin product updates can directly change:

```java
product.setStock(request.getStock());
```

Once a product has `StockLevel` rows, that must no longer be treated as an independent source of truth.

The parent session should inject:

```java
private final StockLevelRepository stockLevelRepository;
```

and, during product update, reject direct stock edits for warehouse-managed products:

```java
if (stockLevelRepository.existsByProductId(productId)
        && request.getStock() != product.getStock()) {
    throw new InvalidOperationException(
            "This product is warehouse-managed. Update stock through the warehouse stock endpoint."
    );
}
```

The warehouse manager's stock endpoint is then the authoritative write path.

---

# 5. Product.stock reconciliation rule

No new inventory field is added to `Product`.

The existing field stays:

```java
private int stock;
```

but its meaning changes for warehouse-managed products:

```text
StockLevel rows
      ↓
SUM(quantity)
      ↓
Product.stock
```

Example:

```text
Store A → Milk → 20
Store B → Milk → 35
Store C → Milk → 10

Product.stock = 65
```

The number `65` is therefore a compatibility aggregate.

A future phase can remove the field completely once every consumer has migrated to store-aware inventory.

---

# 6. Required data provisioning before checkout integration

The new code cannot magically know which physical store contains the existing seeded products.

Before enabling the `OrderService.placeOrder()` integration, the database needs:

```text
Store rows
+
StockLevel rows
```

for the products that should be warehouse-managed.

Example:

```text
Store #1
Delhi Dark Store
lat = ...
lng = ...

StockLevel:
Store #1 + Product #1 = 50
Store #1 + Product #2 = 25
Store #1 + Product #3 = 100
```

The existing `Product.stock` values can be used as the initial quantity when performing the one-time migration into a chosen store.

Do not silently create a fake store or automatically assign all inventory to the nearest store; that would invent physical inventory data.

---

# 7. Role/store provisioning

The existing signup flow intentionally creates:

```text
role = "user"
```

Do not change that into self-service warehouse registration.

For development provisioning, an administrator/database operation can assign:

```sql
UPDATE users
SET role = 'warehouse_manager'
WHERE email = 'manager@example.com';
```

Then assign the manager to a store:

```sql
UPDATE stores
SET warehouse_manager_user_id = (
    SELECT id
    FROM users
    WHERE email = 'manager@example.com'
)
WHERE id = 1;
```

After the role is changed, the user must obtain a new JWT because the role is embedded in the JWT at token creation time.

---

# 8. Files delivered by Phase 18

```text
warehouse/
├── PickItemRequestDTO.java
├── PickList.java
├── PickListItem.java
├── PickListItemResponseDTO.java
├── PickListRepository.java
├── PickListResponseDTO.java
├── PickListStatus.java
├── StockAdjustmentRequestDTO.java
├── StockLevel.java
├── StockLevelRepository.java
├── StockLevelResponseDTO.java
├── Store.java
├── StoreRepository.java
├── StoreResponseDTO.java
├── WarehouseController.java
└── WarehouseService.java
```

No existing `common/` class was recreated.

No existing entity was copied into the new package.

No existing file was silently replaced.

---

# Integrated-delivery note

This copy is included in the final integrated Phase 18-20 archive. The original Phase 18 drop-in package has now been wired to the actual baseline `OrderService`/`AdminService` paths, and Phase 19/20 depend on that integrated warehouse model.
