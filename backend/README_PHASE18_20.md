# NearNow — Integrated Phase 18 + 19 + 20 Backend

This archive is an integrated continuation of the supplied Phase 0-14 NearNow backend baseline.

## Included

- Phase 18 — Warehouse / Dark Store
- Phase 19 — Vendor / Merchant
- Phase 20 — Rider / Delivery Partner
- Cross-domain security wiring
- Warehouse-aware order inventory reservation/restoration
- Vendor ownership wiring into Product
- Admin role/vendor/product/rider provisioning and dispatch
- Rider lifecycle integration with existing OrderStatus

## Important

This is an **integrated source archive**, not a newly generated backend from scratch.

The supplied baseline did not contain `pom.xml`. Therefore Maven compilation, automated tests, and Docker build could not be truthfully executed from the supplied archive.

Read:

```text
docs/INTEGRATION_AND_LIMITATIONS.md
docs/PHASE19_VENDOR.md
docs/PHASE20_RIDER.md
```

before merging/deploying.
