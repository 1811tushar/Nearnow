# NearNow — Integrated Phase 18 + 19 + 20 Delivery Notes

## Baseline used

The source baseline was the supplied:

```text
nearnow-backend-phase0-14-corrected.zip
```

The existing source was treated as the authoritative baseline rather than generating a replacement backend from scratch.

## Integrated domains

```text
Phase 18 — Warehouse / Dark Store
Phase 19 — Vendor / Merchant
Phase 20 — Rider / Delivery Partner
```

The reason Phase 18 is included in the integrated archive is dependency correctness: Phase 20's nearest-rider assignment needs the warehouse `Store`/`PickList` relationship, and Phase 19's stock ownership must respect warehouse `StockLevel` authority.

## Existing files modified

Only these existing application files were intentionally changed for integration:

```text
src/main/java/com/nearnow/common/config/SecurityConfig.java
src/main/java/com/nearnow/product/Product.java
src/main/java/com/nearnow/product/ProductRepository.java
src/main/java/com/nearnow/order/OrderRepository.java
src/main/java/com/nearnow/order/OrderService.java
src/main/java/com/nearnow/admin/AdminService.java
src/main/java/com/nearnow/admin/AdminController.java
```

Existing authentication, JWT and registration behavior were not redesigned.

## New packages

```text
com.nearnow.warehouse
com.nearnow.vendor
com.nearnow.rider
```

## Security additions

```text
/api/warehouse/** → ROLE_WAREHOUSE_MANAGER
/api/vendor/**    → ROLE_VENDOR
/api/rider/**     → ROLE_RIDER
```

Existing admin security remains in place.

## Role provisioning

Public registration remains `role=user`.

Admin can provision:

```text
user
admin
warehouse_manager
vendor
rider
```

A role change is stored in the database, but existing JWTs remain unchanged because the role is embedded in the token. The user must log in again after role assignment.

## Warehouse stock authority

For products with `StockLevel` rows:

```text
StockLevel = authoritative inventory
Product.stock = aggregate compatibility value
```

For products without `StockLevel` rows:

```text
Product.stock = legacy inventory source
```

An order mixing warehouse-managed and legacy products is rejected rather than silently combining two inventory models.

## Vendor ownership

```text
Vendor → Product.vendor
```

Vendor service checks ownership before product mutation.

Warehouse-managed stock cannot be overwritten by vendor/admin product updates.

## Rider assignment

Dispatch requires:

```text
Order.status == PACKED
AND
PickList exists
AND
active/available Rider exists
```

Assignment selection is nearest available rider to the warehouse using Haversine distance, with DB row locking before the final availability mutation.

## Rider payout

Server formula:

```text
₹30 base + ₹8 × (rider→store + store→customer km)
```

No client payout input exists.

Actual bank transfer/disbursement is explicitly out of scope.

## Testing limitation

The supplied archive does not contain `pom.xml`, even though `Dockerfile` expects Maven and `pom.xml`.

Therefore this delivery does **not** claim:

```text
mvn test
mvn compile
Docker build
```

were successfully executed.

A static source inspection was performed, including brace-balance checks and a `javac -proc:none` parse/dependency attempt. That attempt is blocked by the missing dependency classpath, as expected from the missing Maven project file; no syntax error was observed in the added package scan.

## Migration limitation

The baseline uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

There is no Flyway/Liquibase migration system in the supplied archive.

This integrated delivery therefore relies on the existing JPA schema-update behavior for development. Production database migrations should be introduced in the dedicated scale/production-hardening phase rather than silently inventing a migration framework in this feature phase.

## Frontend scope

No React/Next.js dashboard was created.

These phases deliver the role-gated Java REST backend that a future dashboard can consume.

## No new external infrastructure

No new:

- Kafka
- RabbitMQ
- Kubernetes
- PostGIS
- managed maps service
- ML assignment engine

was added.

The current requirements are handled with existing PostgreSQL/Spring infrastructure and deterministic Java logic.
