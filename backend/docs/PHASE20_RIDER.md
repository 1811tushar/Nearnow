# NearNow — Phase 20: Rider / Delivery-Partner Fleet

## Status

**Phase:** 20  
**Feature:** Rider / Delivery Partner fleet  
**Implementation mode:** integrated into the supplied backend baseline + Phase 18 warehouse package  
**Runtime testing:** deferred because the supplied archive has no `pom.xml`.

---

# 0. Verified-before-code findings

Verified existing lifecycle:

```text
PLACED
  ↓
PACKED
  ↓
OUT_FOR_DELIVERY
  ↓
DELIVERED
```

Warehouse Phase 18 produces `PACKED` after pick-list completion.

`Address` already contains latitude/longitude, and `Order.deliveryAddress` snapshots those coordinates.

Therefore Phase 20 can assign riders without a maps API or external routing service.

---

# 1. Problem first

Before Phase 20, an order can become `OUT_FOR_DELIVERY`, but there is no persistent answer to:

> "Which rider is carrying this order?"

Phase 20 adds:

```text
Rider
  ↓
DeliveryAssignment
  ↓
Order
```

---

# 2. Checklist format

## Checkpoint 1 — ENTITY

### `Rider`

Fields:

- `id`
- `user`
- `vehicleType`
- `vehicleNumber`
- `currentLatitude`
- `currentLongitude`
- `active`
- `available`

### `DeliveryAssignment`

Fields:

- `id`
- `order`
- `rider`
- `assignedAt`
- `status`
- `payoutAmount`
- `distanceKm`

Status:

```text
ASSIGNED
PICKED_UP
DELIVERED
```

`payoutAmount` and `distanceKm` are server-generated.

The client never submits the payout.

A unique `order_id` constraint ensures one active assignment record per order in this phase.

---

## Checkpoint 2 — REPOSITORY

Created:

```text
RiderRepository
DeliveryAssignmentRepository
```

Rider lookup includes a pessimistic-lock method:

```text
findByIdForUpdate(...)
```

This is important because two dispatch requests could otherwise both see the same rider as available.

---

## Checkpoint 3 — DTOs

Created:

```text
RiderProfileRequestDTO
RiderLocationRequestDTO
RiderAssignmentStatusRequestDTO
RiderResponseDTO
DeliveryAssignmentResponseDTO
```

The assignment response intentionally exposes payout and distance only after the server has calculated them.

No request DTO contains `payoutAmount`.

---

## Checkpoint 4 — SERVICE

`RiderService` implements:

- rider profile update
- rider GPS location update
- availability update
- assignment listing scoped to the rider
- assignment status transitions
- nearest available rider selection
- DB row locking before rider assignment
- server-side payout calculation
- order lifecycle transition on pickup/delivery

### Assignment algorithm

Problem:

```text
Which rider should receive Order #123?
```

Implementation:

```text
1. Get order
2. Require PACKED
3. Get warehouse PickList → Store
4. Find active + available riders
5. Haversine-sort riders by distance to Store
6. Lock candidates one-by-one
7. Select the first still-available rider
8. Mark rider unavailable
9. Calculate distance-based payout
10. Save DeliveryAssignment
```

No ML, agent, LangChain, or LangGraph is used.

There is no historical dispatch dataset that would justify it.

---

## Payout formula

Current server-owned formula:

```text
BASE_PAYOUT = ₹30
PER_KM      = ₹8

payout = ₹30 + (total route distance × ₹8)
```

`total route distance` is:

```text
rider → warehouse
+
warehouse → customer
```

The value is rounded to two decimal places using `HALF_UP`.

This is a project-level policy constant, not a client-provided number.

---

## Checkpoint 5 — CONTROLLER

### Rider profile

```http
GET /api/rider/profile
PUT /api/rider/profile
```

### Location/availability

```http
PUT /api/rider/location
```

Example:

```json
{
  "latitude": 28.6139,
  "longitude": 77.2090,
  "available": true
}
```

### Assignments

```http
GET /api/rider/assignments
PUT /api/rider/assignments/{id}/status
```

Example pickup:

```json
{
  "status": "PICKED_UP"
}
```

Example completion:

```json
{
  "status": "DELIVERED"
}
```

### Dispatch

The rider does NOT choose themselves for an order.

Admin dispatches:

```http
POST /api/admin/riders/assign/{orderId}
```

Only the order ID is supplied. Rider selection and payout remain server-side.

---

## Checkpoint 6 — SECURITY

Added:

```java
.requestMatchers("/api/rider/**").hasRole("RIDER")
```

Rider assignment reads/writes are scoped by the authenticated user's rider identity.

A rider cannot update another rider's assignment by changing the URL ID.

Admin dispatch remains under the existing `/api/admin/**` role boundary.

---

## Checkpoint 7 — WIRING

Phase 20 depends on Phase 18:

```text
Order
  ↓
PickList
  ↓
Store coordinates
  ↓
nearest Rider
  ↓
DeliveryAssignment
```

Phase 20 also updates the existing Order lifecycle:

```text
PACKED
   ↓ rider pickup
OUT_FOR_DELIVERY
   ↓ rider delivery
DELIVERED
```

---

# 3. Execution Sequence

## Step 0 — Pre-code Q&A

**Why not ML for rider assignment?**

Because nearest-available-rider is a deterministic geometric problem and there is no measured historical optimization problem yet.

**Why not a maps API?**

The project already has latitude/longitude and only needs straight-line distance for the initial assignment algorithm.

**Why not a message broker?**

The locked architecture uses a monolith and Spring-native transaction boundaries for this scale.

**Why is payout calculated in Java?**

Because payout is money. The server must be authoritative.

---

## Step 1 — Entity

Created `Rider`, `DeliveryAssignment`, and status enum.

## Step 2 — Repository

Created rider/assignment repositories with ownership and locking queries.

## Step 3 — DTOs

Created rider and assignment request/response DTOs.

## Step 4 — Service

Created `RiderService` with assignment, payout, location and lifecycle logic.

## Step 5 — Controller

Created `RiderController` and added admin dispatch endpoint.

## Step 6 — Security

Added explicit `ROLE_RIDER` matcher.

## Step 7 — Wiring

Connected rider assignment to Phase 18 `PickList`/`Store` data and existing `OrderStatus`.

## Step 8 — Test

Deferred because the supplied archive has no `pom.xml`.

## Step 9 — Next

Phase 21 can later add WebSocket/SSE live location updates without changing the core assignment model.
