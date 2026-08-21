-- OPTIONAL Phase 18 one-time bootstrap example.
-- Do NOT run blindly in production.
--
-- Purpose:
--   1. create one physical store for development
--   2. copy existing Product.stock into StockLevel for that store
--
-- Before running:
--   - replace the example address/coordinates
--   - confirm which products should be warehouse-managed
--   - confirm the store capacity

INSERT INTO stores (
    name,
    address_line,
    city,
    pincode,
    latitude,
    longitude,
    capacity,
    operating_hours_start,
    operating_hours_end,
    active
)
SELECT
    'NearNow Dark Store 1',
    'Replace with real address',
    'Delhi',
    '110001',
    28.6139,
    77.2090,
    10000,
    '06:00:00',
    '23:00:00',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM stores WHERE name = 'NearNow Dark Store 1'
);

INSERT INTO stock_levels (store_id, product_id, quantity)
SELECT
    s.id,
    p.id,
    p.stock
FROM stores s
CROSS JOIN products p
WHERE s.name = 'NearNow Dark Store 1'
  AND p.active = TRUE
ON CONFLICT (store_id, product_id)
DO NOTHING;

-- After the initial migration, StockLevel is authoritative.
-- Product.stock should be the aggregate of StockLevel rows.
UPDATE products p
SET stock = COALESCE((
    SELECT SUM(sl.quantity)
    FROM stock_levels sl
    WHERE sl.product_id = p.id
), 0)
WHERE EXISTS (
    SELECT 1
    FROM stock_levels sl
    WHERE sl.product_id = p.id
);
